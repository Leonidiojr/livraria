package com.example.livraria.e2e;

import com.example.livraria.DadosTeste;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import static org.assertj.core.api.Assertions.assertThat;

/** Fluxo de venda pela interface: cliente, pedido com itens, pagamento e entrega. */
class VendaE2ETest extends E2ETestBase {

    private static final String LIVRO = "O Pequeno Príncipe"; // livro de exemplo da migracao V3

    private int estoqueAtual() {
        return jdbc.queryForObject("SELECT e.quantidade FROM livraria.estoque e JOIN livraria.livro l USING (id_livro)"
                + " WHERE l.isbn = '999-00-0000-041'", Integer.class);
    }

    private void garantirEstoque(int quantidade) {
        jdbc.update("UPDATE livraria.estoque SET quantidade = GREATEST(quantidade, ?) WHERE id_livro ="
                + " (SELECT id_livro FROM livraria.livro WHERE isbn = '999-00-0000-041')", quantidade);
    }

    private void selecionarLivroNaLinha(int indice, String titulo, int quantidade) {
        WebElement linha = espera.until(d -> {
            var linhas = d.findElements(By.cssSelector("#itens-pedido .item-linha"));
            return linhas.size() > indice ? linhas.get(indice) : null;
        });
        Select livro = new Select(linha.findElement(By.cssSelector(".item-livro")));
        livro.getOptions().stream().filter(o -> o.getText().startsWith(titulo)).findFirst().orElseThrow().click();
        WebElement qtd = linha.findElement(By.cssSelector(".item-qtd"));
        qtd.clear();
        qtd.sendKeys(String.valueOf(quantidade));
    }

    @Test
    @DisplayName("Vendedor cadastra cliente, faz pedido, registra pagamento e entrega; o estoque e baixado")
    void vendaCompleta() {
        String cliente = "E2E Cliente " + unico();
        garantirEstoque(10);
        int estoqueInicial = estoqueAtual();

        // vendedor criado direto no banco de testes via API do admin
        loginComoAdmin();
        abrirAba("usuarios");
        abrirNovo();
        String emailVendedor = "e2e.vendedor" + unico() + "@teste.com";
        digitar("f-nome", "Vendedor E2E");
        digitar("f-email", emailVendedor);
        digitar("f-senha", "vendedor123");
        selecionar("f-perfil", "VENDEDOR");
        salvarEAguardarFechar();
        clicar(By.id("btn-sair"));

        login(emailVendedor, "vendedor123");
        espera.until(ExpectedConditions.urlContains("app.html"));
        aguardarTabela();
        assertThat(id("usuario-perfil").getText()).isEqualTo("VENDEDOR");
        assertThat(driver.findElements(By.cssSelector("button.tab[data-aba='usuarios']"))).isEmpty();

        // cadastra o cliente
        abrirAba("clientes");
        abrirNovo();
        String cpf = DadosTeste.cpf();
        digitar("f-nome", cliente);
        digitar("f-email", DadosTeste.email("e2e.venda"));
        digitar("f-cpf", cpf);
        digitar("f-cidade", "Recife");
        digitar("f-estado", "PE");
        salvarEAguardarFechar();
        assertThat(linhaCom(cliente).getText()).contains(cpf, "Recife/PE");

        // pedido com 2 unidades
        abrirAba("pedidos");
        abrirNovo();
        selecionar("f-clienteId", cliente + " - " + cpf);
        selecionarLivroNaLinha(0, LIVRO, 2);
        espera.until(d -> !d.findElement(By.id("total-pedido")).getText().endsWith("0,00"));
        salvarEAguardarFechar();
        WebElement linha = linhaCom(cliente);
        assertThat(linha.getText()).contains("PENDENTE");
        assertThat(estoqueAtual()).isEqualTo(estoqueInicial - 2);

        // detalhes -> registrar pagamento aprovado -> pedido PAGO
        linha.findElement(By.cssSelector("button[data-acao='ver']")).click();
        aguardarModalAberto();
        assertThat(id("detalhe-itens").getText()).contains(LIVRO);
        clicar(By.id("btn-pagar"));
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("f-formaPagamento")));
        selecionar("f-formaPagamento", "Cartao de credito");
        clicar(By.id("btn-salvar"));
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("detalhe-pagamentos"), "APROVADO"));

        // marcar como enviado e entregue
        clicar(By.cssSelector("[data-status-pedido='ENVIADO']"));
        espera.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-status-pedido='ENTREGUE']")));
        clicar(By.cssSelector("[data-status-pedido='ENTREGUE']"));
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".detalhe"), "ENTREGUE"));
        clicar(By.id("btn-fechar"));

        aguardarTabela();
        assertThat(linhaCom(cliente).getText()).contains("ENTREGUE");

        // o pagamento aparece na aba de pagamentos
        abrirAba("pagamentos");
        buscar(cliente);
        assertThat(linhaCom(cliente).getText()).contains("APROVADO", "Cartao de credito");
    }

    @Test
    @DisplayName("Cliente se cadastra, faz o proprio pedido e o cancela; o estoque volta")
    void clienteFazECancelaPedido() {
        garantirEstoque(5);
        int estoqueInicial = estoqueAtual();
        String email = "e2e.comprador" + unico() + "@teste.com";

        driver.get(url("/index.html"));
        clicar(By.id("link-registro"));
        digitar("reg-nome", "E2E Comprador");
        digitar("reg-email", email);
        digitar("reg-cpf", DadosTeste.cpf());
        digitar("reg-senha", "comprador123");
        clicar(By.id("btn-registrar"));
        el(By.cssSelector("#mensagem.alerta-ok"));
        digitar("login-senha", "comprador123");
        clicar(By.id("btn-entrar"));
        espera.until(ExpectedConditions.urlContains("app.html"));
        aguardarTabela();

        abrirAba("pedidos");
        assertThat(id("btn-novo").getText()).isEqualTo("Novo pedido");
        abrirNovo();
        assertThat(driver.findElements(By.id("f-clienteId"))).isEmpty(); // compra para si mesmo
        selecionarLivroNaLinha(0, LIVRO, 1);
        clicar(By.id("btn-salvar"));
        // apos criar o pedido o cliente vai para o pagamento; aqui ele desiste e paga depois
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("btn-salvar"), "Confirmar pagamento"));
        clicar(By.id("btn-cancelar"));
        aguardarTabela();
        assertThat(linhaCom("E2E Comprador").getText()).contains("PENDENTE");
        assertThat(linhaCom("E2E Comprador").findElements(By.cssSelector("button[data-acao='pagar']"))).hasSize(1);
        assertThat(estoqueAtual()).isEqualTo(estoqueInicial - 1);
        // cliente nao edita nem exclui
        assertThat(linhaCom("E2E Comprador").findElements(By.cssSelector("button[data-acao='editar']"))).isEmpty();

        linhaCom("E2E Comprador").findElement(By.cssSelector("button[data-acao='ver']")).click();
        aguardarModalAberto();
        assertThat(driver.findElements(By.id("btn-pagar"))).isEmpty();
        clicar(By.cssSelector("[data-status-pedido='CANCELADO']"));
        espera.until(ExpectedConditions.alertIsPresent()).accept();
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".detalhe"), "CANCELADO"));

        assertThat(estoqueAtual()).isEqualTo(estoqueInicial);
    }

    @Test
    @DisplayName("Cliente faz o pedido e paga em seguida; o pedido fica PAGO")
    void clienteFazEPagaPedido() {
        garantirEstoque(5);
        String email = "e2e.pagador" + unico() + "@teste.com";

        driver.get(url("/index.html"));
        clicar(By.id("link-registro"));
        digitar("reg-nome", "E2E Pagador");
        digitar("reg-email", email);
        digitar("reg-cpf", DadosTeste.cpf());
        digitar("reg-senha", "pagador123");
        clicar(By.id("btn-registrar"));
        el(By.cssSelector("#mensagem.alerta-ok"));
        digitar("login-senha", "pagador123");
        clicar(By.id("btn-entrar"));
        espera.until(ExpectedConditions.urlContains("app.html"));
        aguardarTabela();

        abrirAba("pedidos");
        abrirNovo();
        selecionarLivroNaLinha(0, LIVRO, 1);
        clicar(By.id("btn-salvar"));
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.id("btn-salvar"), "Confirmar pagamento"));
        clicar(By.cssSelector("input[name='formaPagamento'][value='CARTAO_CREDITO']"));
        clicar(By.id("btn-salvar"));

        // ao confirmar, o detalhe do pedido e exibido ja pago
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".detalhe"), "PAGO"));
        assertThat(id("detalhe-pagamentos").getText()).contains("Cartao de credito").contains("APROVADO");
        assertThat(driver.findElements(By.id("btn-pagar-cliente"))).isEmpty();
        clicar(By.id("btn-cancelar"));
        aguardarTabela();
        assertThat(linhaCom("E2E Pagador").getText()).contains("PAGO");
        assertThat(linhaCom("E2E Pagador").findElements(By.cssSelector("button[data-acao='pagar']"))).isEmpty();
    }

    @Test
    @DisplayName("Pedido acima do estoque mostra a mensagem de estoque insuficiente")
    void estoqueInsuficiente() {
        String cliente = "E2E Sem Estoque " + unico();
        loginComoAdmin();
        abrirAba("clientes");
        abrirNovo();
        digitar("f-nome", cliente);
        String cpf = DadosTeste.cpf();
        digitar("f-email", DadosTeste.email("e2e.semestoque"));
        digitar("f-cpf", cpf);
        salvarEAguardarFechar();

        abrirAba("pedidos");
        abrirNovo();
        selecionar("f-clienteId", cliente + " - " + cpf);
        selecionarLivroNaLinha(0, LIVRO, 99999);
        clicar(By.id("btn-salvar"));

        assertThat(el(By.cssSelector("#modal-erro:not(.oculto)")).getText()).contains("Estoque insuficiente");
    }

    @Test
    @DisplayName("Admin cadastra editora e categoria; auditoria registra as operacoes")
    void catalogoEAuditoria() {
        String editora = "E2E Editora " + unico();
        String categoria = "E2E Categoria " + unico();
        loginComoAdmin();

        abrirAba("editoras");
        abrirNovo();
        digitar("f-nome", editora);
        digitar("f-cnpj", "abc");
        clicar(By.id("btn-salvar"));
        assertThat(el(By.cssSelector("[data-campo='cnpj']")).getText()).contains("CNPJ");
        digitar("f-cnpj", "");
        digitar("f-site", "https://e2e.example");
        salvarEAguardarFechar();
        assertThat(linhaCom(editora).getText()).contains("https://e2e.example");

        abrirAba("categorias");
        abrirNovo();
        digitar("f-nome", categoria);
        salvarEAguardarFechar();
        excluirLinha(categoria);

        abrirAba("auditoria");
        new Select(id("filtro")).selectByValue("categoria");
        aguardarTabela();
        buscar(categoria);
        WebElement exclusao = linhaCom("DELETE");
        assertThat(exclusao.getText()).contains("admin@livraria.com");
        exclusao.findElement(By.cssSelector("button[data-acao='ver']")).click();
        aguardarModalAberto();
        assertThat(id("modal-campos").getText()).contains(categoria);
    }
}
