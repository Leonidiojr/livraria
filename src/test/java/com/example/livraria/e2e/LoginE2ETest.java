package com.example.livraria.e2e;

import com.example.livraria.DadosTeste;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.assertj.core.api.Assertions.assertThat;

class LoginE2ETest extends E2ETestBase {

    @Test
    @DisplayName("Admin faz login, ve o painel com seu nome e perfil e consegue sair")
    void loginELogout() {
        loginComoAdmin();

        assertThat(id("usuario-nome").getText()).isEqualTo("Administrador");
        assertThat(id("usuario-perfil").getText()).isEqualTo("ADMIN");
        assertThat(id("titulo-aba").getText()).isEqualTo("Livros");
        assertThat(id("btn-novo").isDisplayed()).isTrue();
        assertThat(el(By.cssSelector("button.tab[data-aba='usuarios']")).isDisplayed()).isTrue();

        clicar(By.id("btn-sair"));
        espera.until(ExpectedConditions.urlContains("index.html"));
        assertThat(id("form-login").isDisplayed()).isTrue();
    }

    @Test
    @DisplayName("Senha incorreta exibe mensagem de erro e permanece na tela de login")
    void senhaIncorreta() {
        login(ADMIN_EMAIL, "senha-errada");

        assertThat(el(By.cssSelector("#mensagem.alerta-erro")).getText()).contains("E-mail ou senha invalidos");
        assertThat(driver.getCurrentUrl()).contains("index.html");
    }

    @Test
    @DisplayName("Campos vazios nao enviam o formulario")
    void camposVazios() {
        driver.get(url("/index.html"));
        clicar(By.id("btn-entrar"));

        assertThat(el(By.cssSelector("#mensagem.alerta-erro")).getText()).contains("Informe e-mail e senha");
    }

    @Test
    @DisplayName("Acessar o painel sem login redireciona para a tela de login")
    void painelExigeLogin() {
        driver.get(url("/app.html"));

        espera.until(ExpectedConditions.urlContains("index.html"));
        assertThat(id("form-login").isDisplayed()).isTrue();
    }

    @Test
    @DisplayName("Visitante se cadastra, entra como CLIENTE e nao ve as acoes de administrador")
    void autoCadastroCliente() {
        String email = "e2e.cliente" + unico() + "@teste.com";
        driver.get(url("/index.html"));
        clicar(By.id("link-registro"));
        digitar("reg-nome", "Cliente E2E");
        digitar("reg-email", email);
        digitar("reg-cpf", DadosTeste.cpf());
        digitar("reg-senha", "cliente123");
        clicar(By.id("btn-registrar"));

        assertThat(el(By.cssSelector("#mensagem.alerta-ok")).getText()).contains("Conta criada");
        assertThat(id("login-email").getAttribute("value")).isEqualTo(email);

        digitar("login-senha", "cliente123");
        clicar(By.id("btn-entrar"));
        espera.until(ExpectedConditions.urlContains("app.html"));
        aguardarTabela();

        assertThat(id("usuario-perfil").getText()).isEqualTo("CLIENTE");
        assertThat(driver.findElement(By.id("btn-novo")).isDisplayed()).isFalse();
        // abas de administracao/operacao nem sao exibidas para o CLIENTE
        assertThat(driver.findElements(By.cssSelector("button.tab[data-aba='usuarios']"))).isEmpty();
        assertThat(driver.findElements(By.cssSelector("button.tab[data-aba='estoque']"))).isEmpty();
        assertThat(driver.findElements(By.cssSelector("button.tab[data-aba='auditoria']"))).isEmpty();
        assertThat(driver.findElements(By.cssSelector("button.tab[data-aba='pedidos']"))).hasSize(1);
        // cliente nao edita nem exclui livros (pode apenas comprar)
        assertThat(driver.findElements(By.cssSelector(
                "#tabela-corpo button[data-acao='editar'], #tabela-corpo button[data-acao='excluir']"))).isEmpty();
    }

    @Test
    @DisplayName("Cadastro com senha curta mostra o erro do campo vindo da API")
    void cadastroInvalido() {
        driver.get(url("/index.html"));
        clicar(By.id("link-registro"));
        digitar("reg-nome", "Cliente E2E");
        digitar("reg-email", "e2e.curta" + unico() + "@teste.com");
        digitar("reg-cpf", DadosTeste.cpf());
        digitar("reg-senha", "123");
        clicar(By.id("btn-registrar"));

        assertThat(el(By.cssSelector("#form-registro [data-campo='senha']")).getText())
                .contains("entre 6 e 72");
    }
}
