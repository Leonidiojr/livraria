package com.example.livraria.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.assertj.core.api.Assertions.assertThat;

class LivroE2ETest extends E2ETestBase {

    private void criarAutorPelaTela(String nome) {
        abrirAba("autores");
        abrirNovo();
        digitar("f-nome", nome);
        salvarEAguardarFechar();
        linhaCom(nome);
    }

    private void preencherLivro(String isbn, String titulo, String preco, String autor) {
        digitar("f-isbn", isbn);
        digitar("f-titulo", titulo);
        digitar("f-preco", preco);
        digitar("f-anoPublicacao", "2021");
        digitar("f-idioma", "Portugues");
        selecionar("f-categoriaId", "Tecnologia");
        selecionar("f-editoraId", "Novatec");
        if (autor != null) {
            selecionar("f-autoresIds", autor);
        }
    }

    @Test
    @DisplayName("CRUD completo de livro pela interface: cadastrar, buscar, editar e excluir")
    void crudCompleto() {
        String u = unico();
        String autor = "E2E Autor " + u;
        String titulo = "E2E Livro " + u;
        String isbn = "978-" + String.format("%010d", Long.parseLong(u));

        loginComoAdmin();
        criarAutorPelaTela(autor);

        // CREATE
        abrirAba("livros");
        abrirNovo();
        preencherLivro(isbn, titulo, "59.90", autor);
        driver.findElement(By.id("f-capa")).sendKeys(com.example.livraria.Imagens.arquivoPng().toString());
        salvarEAguardarFechar();
        assertThat(textoToast()).contains("salvo com sucesso");
        assertThat(linhaCom(titulo).findElement(By.cssSelector("img.capa-mini")).getAttribute("src")).contains("/capa?v=");

        // READ (listagem + busca por autor)
        WebElement linha = linhaCom(titulo);
        assertThat(linha.getText()).contains(autor, "Tecnologia", "Novatec", "59,90", "Ativo");
        buscar(autor);
        assertThat(existeLinhaCom(titulo)).isTrue();

        // UPDATE
        editarLinha(titulo);
        assertThat(id("f-titulo").getAttribute("value")).isEqualTo(titulo);
        digitar("f-titulo", titulo + " Revisado");
        digitar("f-preco", "75");
        id("f-ativo").click(); // desmarca "ativo"
        salvarEAguardarFechar();

        linha = linhaCom(titulo + " Revisado");
        assertThat(linha.getText()).contains("75,00", "Inativo");

        // DELETE
        excluirLinha(titulo + " Revisado");
        assertThat(textoToast()).contains("excluido");
        buscar(titulo);
        assertThat(el(By.cssSelector("#tabela-corpo td.vazio")).getText()).contains("Nenhum livro encontrado");
    }

    @Test
    @DisplayName("Salvar sem os campos obrigatorios mostra 'Campo obrigatorio' e mantem o formulario aberto")
    void camposObrigatorios() {
        loginComoAdmin();
        abrirNovo();
        clicar(By.id("btn-salvar"));

        assertThat(el(By.cssSelector("[data-campo='isbn']")).getText()).isEqualTo("Campo obrigatorio");
        assertThat(el(By.cssSelector("[data-campo='titulo']")).getText()).isEqualTo("Campo obrigatorio");
        assertThat(el(By.cssSelector("[data-campo='categoriaId']")).getText()).isEqualTo("Campo obrigatorio");
        assertThat(id("modal").isDisplayed()).isTrue();
    }

    @Test
    @DisplayName("ISBN invalido e ISBN duplicado exibem os erros retornados pela API")
    void errosDaApi() {
        String u = unico();
        String isbn = "978-" + String.format("%010d", Long.parseLong(u));
        loginComoAdmin();

        abrirNovo();
        preencherLivro("abc", "E2E Invalido " + u, "10", null);
        clicar(By.id("btn-salvar"));
        assertThat(el(By.cssSelector("[data-campo='isbn']")).getText()).contains("ISBN invalido");
        clicar(By.id("btn-cancelar"));

        abrirNovo();
        preencherLivro(isbn, "E2E Original " + u, "10", null);
        salvarEAguardarFechar();

        abrirNovo();
        preencherLivro(isbn, "E2E Copia " + u, "10", null);
        clicar(By.id("btn-salvar"));
        assertThat(el(By.cssSelector("#modal-erro:not(.oculto)")).getText()).contains("Ja existe um livro com o ISBN");
    }
}
