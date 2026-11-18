package com.example.livraria.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioE2ETest extends E2ETestBase {

    @Test
    @DisplayName("Admin cadastra usuario, o usuario consegue entrar, e depois e editado e excluido")
    void crudCompleto() {
        String email = "e2e.vendedor" + unico() + "@teste.com";
        loginComoAdmin();
        abrirAba("usuarios");

        // CREATE
        abrirNovo();
        digitar("f-nome", "Vendedor E2E");
        digitar("f-email", email);
        digitar("f-senha", "vendedor123");
        selecionar("f-perfil", "CLIENTE");
        salvarEAguardarFechar();
        assertThat(linhaCom(email).getText()).contains("Vendedor E2E", "CLIENTE", "Ativo");

        // o novo usuario consegue autenticar
        clicar(By.id("btn-sair"));
        login(email, "vendedor123");
        espera.until(ExpectedConditions.urlContains("app.html"));
        assertThat(id("usuario-nome").getText()).isEqualTo("Vendedor E2E");
        clicar(By.id("btn-sair"));

        // UPDATE: promove a ADMIN e desativa
        loginComoAdmin();
        abrirAba("usuarios");
        editarLinha(email);
        assertThat(id("f-senha").getAttribute("value")).isEmpty();
        digitar("f-nome", "Vendedor E2E Editado");
        selecionar("f-perfil", "ADMIN");
        id("f-ativo").click();
        salvarEAguardarFechar();
        assertThat(linhaCom(email).getText()).contains("Vendedor E2E Editado", "ADMIN", "Inativo");

        // usuario inativo nao entra mais
        clicar(By.id("btn-sair"));
        login(email, "vendedor123");
        assertThat(el(By.cssSelector("#mensagem.alerta-erro")).getText()).contains("E-mail ou senha invalidos");

        // DELETE
        loginComoAdmin();
        abrirAba("usuarios");
        excluirLinha(email);
        assertThat(existeLinhaCom(email)).isFalse();
    }

    @Test
    @DisplayName("Admin nao consegue excluir a propria conta (mensagem de erro na tela)")
    void naoExcluiASiMesmo() {
        loginComoAdmin();
        abrirAba("usuarios");

        linhaCom(ADMIN_EMAIL).findElement(By.cssSelector("button[data-acao='excluir']")).click();
        espera.until(ExpectedConditions.alertIsPresent()).accept();

        assertThat(el(By.cssSelector("#mensagem.alerta-erro")).getText()).contains("nao pode excluir o proprio usuario");
        assertThat(existeLinhaCom(ADMIN_EMAIL)).isTrue();
    }

    @Test
    @DisplayName("E-mail ja cadastrado exibe o erro de conflito no formulario")
    void emailDuplicado() {
        loginComoAdmin();
        abrirAba("usuarios");
        abrirNovo();
        digitar("f-nome", "Duplicado");
        digitar("f-email", ADMIN_EMAIL);
        digitar("f-senha", "senha123");
        salvarSemFechar();

        assertThat(el(By.cssSelector("#modal-erro:not(.oculto)")).getText()).contains("Ja existe um usuario");
    }

    private void salvarSemFechar() {
        clicar(By.id("btn-salvar"));
    }
}
