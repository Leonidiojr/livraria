package com.example.livraria.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AutorE2ETest extends E2ETestBase {

    @Test
    @DisplayName("CRUD completo de autor pela interface")
    void crudCompleto() {
        String nome = "E2E Autora " + unico();
        loginComoAdmin();
        abrirAba("autores");

        abrirNovo();
        digitar("f-nome", nome);
        digitar("f-nacionalidade", "Portuguesa");
        digitar("f-biografia", "Biografia criada pelo teste automatizado.");
        // foto do autor: pre-visualizacao aparece antes de salvar
        driver.findElement(org.openqa.selenium.By.id("f-foto")).sendKeys(com.example.livraria.Imagens.arquivoPng().toString());
        el(org.openqa.selenium.By.cssSelector("#previa-foto img"));
        salvarEAguardarFechar();
        assertThat(linhaCom(nome).getText()).contains("Portuguesa", "Biografia criada");
        org.openqa.selenium.WebElement foto = linhaCom(nome).findElement(org.openqa.selenium.By.cssSelector("img.foto-mini"));
        assertThat(foto.getAttribute("src")).contains("/foto?v=");
        // a imagem realmente carregou no navegador
        espera.until(d -> (Boolean) ((org.openqa.selenium.JavascriptExecutor) d)
                .executeScript("return arguments[0].complete && arguments[0].naturalWidth > 0", foto));

        // remover a foto volta para as iniciais
        editarLinha(nome);
        clicar(org.openqa.selenium.By.id("f-foto-remover"));
        salvarEAguardarFechar();
        assertThat(linhaCom(nome).findElements(org.openqa.selenium.By.cssSelector(".foto-vazia"))).hasSize(1);

        buscar(nome);
        assertThat(existeLinhaCom(nome)).isTrue();

        editarLinha(nome);
        digitar("f-nacionalidade", "Brasileira");
        salvarEAguardarFechar();
        assertThat(linhaCom(nome).getText()).contains("Brasileira");

        excluirLinha(nome);
        assertThat(existeLinhaCom(nome)).isFalse();
    }

    @Test
    @DisplayName("Cancelar a exclusao mantem o autor na lista")
    void cancelarExclusao() {
        String nome = "E2E Mantido " + unico();
        loginComoAdmin();
        abrirAba("autores");
        abrirNovo();
        digitar("f-nome", nome);
        salvarEAguardarFechar();

        linhaCom(nome).findElement(org.openqa.selenium.By.cssSelector("button[data-acao='excluir']")).click();
        espera.until(org.openqa.selenium.support.ui.ExpectedConditions.alertIsPresent()).dismiss();

        aguardarTabela();
        assertThat(existeLinhaCom(nome)).isTrue();
    }
}
