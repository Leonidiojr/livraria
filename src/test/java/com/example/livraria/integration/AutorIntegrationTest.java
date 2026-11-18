package com.example.livraria.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AutorIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("CRUD completo de autor")
    void crudCompleto() throws Exception {
        String token = tokenAdmin();

        String criado = mvc.perform(json(post("/api/autores"), token, """
                        {"nome": "Cecilia Meireles", "nacionalidade": "Brasileira",
                         "dataNascimento": "1901-11-07", "biografia": "Poetisa"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dataNascimento").value("1901-11-07"))
                .andReturn().getResponse().getContentAsString();
        Integer autorId = id(criado);

        mvc.perform(comToken(get("/api/autores").param("q", "cecilia"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(autorId)));

        mvc.perform(json(put("/api/autores/" + autorId), token, """
                        {"nome": "Cecilia Meireles", "nacionalidade": "Brasileira", "biografia": "Poetisa e professora"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biografia").value("Poetisa e professora"))
                .andExpect(jsonPath("$.dataNascimento").doesNotExist());

        mvc.perform(comToken(delete("/api/autores/" + autorId), token))
                .andExpect(status().isNoContent());
        mvc.perform(comToken(get("/api/autores"), token))
                .andExpect(jsonPath("$[*].id", not(hasItem(autorId))));
    }

    @Test
    @DisplayName("Excluir autor vinculado a livro remove apenas o vinculo")
    void excluirAutorComLivro() throws Exception {
        String token = tokenAdmin();
        Integer autorId = id(mvc.perform(json(post("/api/autores"), token, """
                        {"nome": "Autor Vinculado"}"""))
                .andReturn().getResponse().getContentAsString());
        Integer livroId = id(mvc.perform(json(post("/api/livros"), token, """
                        {"isbn": "978-1111111111", "titulo": "Livro do Autor", "preco": 20,
                         "categoriaId": 1, "editoraId": 1, "autoresIds": [%d]}""".formatted(autorId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        mvc.perform(comToken(delete("/api/autores/" + autorId), token)).andExpect(status().isNoContent());

        mvc.perform(comToken(get("/api/livros/" + livroId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autores").isEmpty());
    }

    @Test
    @DisplayName("Data de nascimento no futuro e nome vazio retornam 400")
    void validacao() throws Exception {
        mvc.perform(json(post("/api/autores"), tokenAdmin(), """
                        {"nome": " ", "dataNascimento": "2999-01-01"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.dataNascimento").exists());
    }

    @Test
    @DisplayName("Autor inexistente retorna 404")
    void inexistente() throws Exception {
        String token = tokenAdmin();
        mvc.perform(comToken(get("/api/autores/999999"), token)).andExpect(status().isNotFound());
        mvc.perform(comToken(delete("/api/autores/999999"), token)).andExpect(status().isNotFound());
    }
}
