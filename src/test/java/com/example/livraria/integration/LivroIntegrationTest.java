package com.example.livraria.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LivroIntegrationTest extends IntegrationTestBase {

    private static final String LIVRO = """
            {"isbn": "%s", "titulo": "%s", "preco": 49.90, "categoriaId": 1, "editoraId": 1,
             "anoPublicacao": 2020, "numeroPaginas": 320, "idioma": "Portugues", "autoresIds": [%s]}""";

    private Integer criarAutor(String token, String nome) throws Exception {
        String r = mvc.perform(json(post("/api/autores"), token, """
                        {"nome": "%s", "nacionalidade": "Brasileira"}""".formatted(nome)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return id(r);
    }

    @Test
    @DisplayName("CRUD completo de livro: criar, ler, buscar, atualizar e excluir")
    void crudCompleto() throws Exception {
        String token = tokenAdmin();
        Integer autorId = criarAutor(token, "Autora Integracao");

        // CREATE
        String criado = mvc.perform(json(post("/api/livros"), token,
                        LIVRO.formatted("978-0000000001", "Livro de Integracao", autorId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.titulo").value("Livro de Integracao"))
                .andExpect(jsonPath("$.preco").value(49.90))
                .andExpect(jsonPath("$.categoriaNome").isNotEmpty())
                .andExpect(jsonPath("$.autores[0].nome").value("Autora Integracao"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andReturn().getResponse().getContentAsString();
        Integer livroId = id(criado);

        // READ por id
        mvc.perform(comToken(get("/api/livros/" + livroId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("978-0000000001"));

        // READ lista com busca por titulo e por autor
        mvc.perform(comToken(get("/api/livros").param("q", "integracao"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(livroId)));
        mvc.perform(comToken(get("/api/livros").param("q", "Autora"), token))
                .andExpect(jsonPath("$[*].id", hasItem(livroId)));

        // UPDATE
        mvc.perform(json(put("/api/livros/" + livroId), token, """
                        {"isbn": "978-0000000001", "titulo": "Titulo Alterado", "preco": 10, "categoriaId": 2,
                         "editoraId": 2, "autoresIds": [], "ativo": false}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Titulo Alterado"))
                .andExpect(jsonPath("$.preco").value(10.0))
                .andExpect(jsonPath("$.autores", hasSize(0)))
                .andExpect(jsonPath("$.ativo").value(false));

        // DELETE
        mvc.perform(comToken(delete("/api/livros/" + livroId), token))
                .andExpect(status().isNoContent());
        mvc.perform(comToken(get("/api/livros/" + livroId), token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("ISBN duplicado retorna 409")
    void isbnDuplicado() throws Exception {
        String token = tokenAdmin();
        mvc.perform(json(post("/api/livros"), token, LIVRO.formatted("978-0000000002", "Primeiro", "")))
                .andExpect(status().isCreated());
        mvc.perform(json(post("/api/livros"), token, LIVRO.formatted("978-0000000002", "Segundo", "")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem", containsString("ISBN")));
    }

    @Test
    @DisplayName("Dados invalidos retornam 400 com a lista de campos")
    void validacao() throws Exception {
        mvc.perform(json(post("/api/livros"), tokenAdmin(), """
                        {"isbn": "abc", "titulo": "", "preco": 0}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.isbn").exists())
                .andExpect(jsonPath("$.campos.titulo").exists())
                .andExpect(jsonPath("$.campos.preco").exists())
                .andExpect(jsonPath("$.campos.categoriaId").exists())
                .andExpect(jsonPath("$.campos.editoraId").exists());
    }

    @Test
    @DisplayName("Categoria inexistente retorna 404")
    void categoriaInexistente() throws Exception {
        mvc.perform(json(post("/api/livros"), tokenAdmin(), """
                        {"isbn": "978-0000000003", "titulo": "X", "preco": 1, "categoriaId": 99999, "editoraId": 1}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CLIENTE pode listar livros mas nao pode criar, alterar ou excluir (403)")
    void permissoesCliente() throws Exception {
        String cliente = tokenCliente();
        mvc.perform(comToken(get("/api/livros"), cliente)).andExpect(status().isOk());
        mvc.perform(json(post("/api/livros"), cliente, LIVRO.formatted("978-0000000004", "X", "")))
                .andExpect(status().isForbidden());
        mvc.perform(json(put("/api/livros/1"), cliente, LIVRO.formatted("978-0000000004", "X", "")))
                .andExpect(status().isForbidden());
        mvc.perform(comToken(delete("/api/livros/1"), cliente)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Listas de apoio (categorias e editoras) vem das migracoes")
    void listasDeApoio() throws Exception {
        String token = tokenAdmin();
        mvc.perform(comToken(get("/api/categorias"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", hasItems("Romance", "Tecnologia")));
        mvc.perform(comToken(get("/api/editoras"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))));
    }
}
