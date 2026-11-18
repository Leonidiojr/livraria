package com.example.livraria.e2e;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;

/**
 * Base dos testes ponta a ponta (E2E): sobe a aplicacao em uma porta real e controla
 * um Google Chrome (headless) com Selenium WebDriver, exatamente como um usuario faria.
 *
 * <p>Todos os dados criados pelos testes usam o prefixo "E2E" (ou e-mails "e2e.") e sao
 * apagados apos cada teste. Para ver o navegador durante a execucao use {@code -De2e.headless=false}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("e2e")
abstract class E2ETestBase {

    static final String ADMIN_EMAIL = "admin@livraria.com";
    static final String ADMIN_SENHA = "admin123";

    private static WebDriver driverCompartilhado;

    @LocalServerPort
    int porta;

    @Autowired
    JdbcTemplate jdbc;

    WebDriver driver;
    WebDriverWait espera;

    @BeforeAll
    static void abrirNavegador() {
        ChromeOptions opcoes = new ChromeOptions();
        if (Boolean.parseBoolean(System.getProperty("e2e.headless", "true"))) {
            opcoes.addArguments("--headless=new");
        }
        opcoes.addArguments("--window-size=1280,900", "--lang=pt-BR", "--disable-search-engine-choice-screen");
        try {
            driverCompartilhado = new ChromeDriver(opcoes);
        } catch (Exception e) {
            Assumptions.abort("Google Chrome/ChromeDriver indisponivel, testes E2E ignorados: " + e.getMessage());
        }
    }

    @AfterAll
    static void fecharNavegador() {
        if (driverCompartilhado != null) {
            driverCompartilhado.quit();
            driverCompartilhado = null;
        }
    }

    @BeforeEach
    void prepararNavegador() {
        driver = driverCompartilhado;
        espera = new WebDriverWait(driver, Duration.ofSeconds(10));
        // a tabela e redesenhada apos cada operacao: elementos antigos ficam "stale" e a espera tenta de novo
        espera.ignoring(StaleElementReferenceException.class);
        // garante que cada teste comeca deslogado
        driver.get(url("/index.html"));
        ((JavascriptExecutor) driver).executeScript("sessionStorage.clear();");
    }

    @AfterEach
    void limparDadosDeTeste() {
        limparDadosE2E(jdbc);
    }

    /** Remove tudo que os testes E2E criaram, respeitando as chaves estrangeiras. */
    static void limparDadosE2E(JdbcTemplate jdbc) {
        String clientes = "SELECT id_cliente FROM livraria.cliente WHERE nome LIKE 'E2E%' OR email LIKE 'e2e.%'";
        String livros = "SELECT id_livro FROM livraria.livro WHERE titulo LIKE 'E2E%'";
        String pedidos = "SELECT id_pedido FROM livraria.pedido WHERE id_cliente IN (" + clientes + ")"
                + " UNION SELECT id_pedido FROM livraria.item_pedido WHERE id_livro IN (" + livros + ")";
        // devolve ao estoque o que os pedidos de teste retiraram dos livros reais
        jdbc.update("UPDATE livraria.estoque e SET quantidade = e.quantidade + x.qtd FROM ("
                + " SELECT i.id_livro, SUM(i.quantidade) qtd FROM livraria.item_pedido i JOIN livraria.pedido p USING (id_pedido)"
                + " WHERE p.status <> 'CANCELADO' AND i.id_pedido IN (" + pedidos + ") GROUP BY i.id_livro) x"
                + " WHERE e.id_livro = x.id_livro");
        jdbc.update("DELETE FROM livraria.pagamento WHERE id_pedido IN (" + pedidos + ")");
        jdbc.update("DELETE FROM livraria.pedido WHERE id_pedido IN (" + pedidos + ")");
        jdbc.update("DELETE FROM livraria.estoque WHERE id_livro IN (" + livros + ")");
        jdbc.update("DELETE FROM livraria.livro WHERE titulo LIKE 'E2E%'");
        jdbc.update("DELETE FROM livraria.autor WHERE nome LIKE 'E2E%'");
        jdbc.update("DELETE FROM livraria.editora WHERE nome LIKE 'E2E%'");
        jdbc.update("DELETE FROM livraria.categoria WHERE nome LIKE 'E2E%'");
        jdbc.update("DELETE FROM livraria.cliente WHERE id_cliente IN (" + clientes + ")");
        jdbc.update("DELETE FROM livraria.usuario WHERE email LIKE 'e2e.%'");
    }

    // ------------------------------------------------------------------ utilitarios

    String url(String caminho) {
        return "http://localhost:" + porta + caminho;
    }

    /** Sufixo unico para evitar colisao de dados entre execucoes. */
    static String unico() {
        return Long.toString(System.nanoTime() % 1_000_000_000L);
    }

    WebElement el(By by) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    WebElement id(String id) {
        return el(By.id(id));
    }

    void digitar(String campoId, String texto) {
        WebElement e = id(campoId);
        e.clear();
        e.sendKeys(texto);
    }

    void clicar(By by) {
        espera.until(ExpectedConditions.elementToBeClickable(by)).click();
    }

    void selecionar(String campoId, String textoVisivel) {
        new Select(id(campoId)).selectByVisibleText(textoVisivel);
    }

    // ------------------------------------------------------------------ fluxos comuns

    void login(String email, String senha) {
        driver.get(url("/index.html"));
        digitar("login-email", email);
        digitar("login-senha", senha);
        clicar(By.id("btn-entrar"));
    }

    void loginComoAdmin() {
        login(ADMIN_EMAIL, ADMIN_SENHA);
        espera.until(ExpectedConditions.urlContains("app.html"));
        aguardarTabela();
    }

    void abrirAba(String aba) {
        clicar(By.cssSelector("button.tab[data-aba='" + aba + "']"));
        espera.until(ExpectedConditions.attributeContains(
                By.cssSelector("button.tab[data-aba='" + aba + "']"), "class", "ativa"));
        aguardarTabela();
    }

    void aguardarTabela() {
        espera.until(d -> !d.findElement(By.id("tabela-corpo")).getText().contains("Carregando"));
    }

    void buscar(String termo) {
        digitar("campo-busca", termo);
        clicar(By.cssSelector("#form-busca button[type=submit]"));
        aguardarTabela();
    }

    void abrirNovo() {
        clicar(By.id("btn-novo"));
        aguardarModalAberto();
    }

    void aguardarModalAberto() {
        espera.until(d -> Boolean.TRUE.equals(
                ((JavascriptExecutor) d).executeScript("return document.getElementById('modal').open")));
    }

    void salvarEAguardarFechar() {
        clicar(By.id("btn-salvar"));
        espera.until(d -> Boolean.FALSE.equals(
                ((JavascriptExecutor) d).executeScript("return document.getElementById('modal').open")));
        aguardarTabela();
    }

    /** Linha da tabela que contem o texto informado (espera ate aparecer). */
    WebElement linhaCom(String texto) {
        return espera.until(d -> d.findElements(By.cssSelector("#tabela-corpo tr")).stream()
                .filter(tr -> tr.getText().contains(texto))
                .findFirst().orElse(null));
    }

    /** Le todas as linhas em uma unica chamada JS, imune a re-renderizacao da tabela. */
    boolean existeLinhaCom(String texto) {
        @SuppressWarnings("unchecked")
        List<String> linhas = (List<String>) ((JavascriptExecutor) driver).executeScript(
                "return [...document.querySelectorAll('#tabela-corpo tr')].map(tr => tr.innerText);");
        return linhas.stream().anyMatch(t -> t.contains(texto));
    }

    void editarLinha(String texto) {
        linhaCom(texto).findElement(By.cssSelector("button[data-acao='editar']")).click();
        aguardarModalAberto();
    }

    void excluirLinha(String texto) {
        linhaCom(texto).findElement(By.cssSelector("button[data-acao='excluir']")).click();
        espera.until(ExpectedConditions.alertIsPresent()).accept();
        espera.until(d -> !existeLinhaCom(texto));
    }

    String textoToast() {
        return el(By.cssSelector("#toast:not(.oculto)")).getText();
    }
}
