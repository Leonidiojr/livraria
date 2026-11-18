package com.example.livraria.integration;

import com.example.livraria.DadosTeste;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de integracao: sobe a aplicacao completa (seguranca, JPA, Flyway)
 * contra o banco PostgreSQL de testes. Cada teste roda em uma transacao desfeita ao final.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class IntegrationTestBase {

    static final String ADMIN_EMAIL = "admin@livraria.com";
    static final String ADMIN_SENHA = "admin123";

    @Autowired
    protected MockMvc mvc;

    protected String login(String email, String senha) throws Exception {
        String corpo = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "senha": "%s"}""".formatted(email, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.token");
    }

    protected String tokenAdmin() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_SENHA);
    }

    /** Cria um usuario CLIENTE pelo endpoint publico de registro e devolve o token dele. */
    protected String tokenCliente() throws Exception {
        mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Cliente Teste", "email": "cliente.it@teste.com", "cpf": "%s", "senha": "cliente123"}"""
                                .formatted(DadosTeste.cpf())))
                .andExpect(status().isCreated());
        return login("cliente.it@teste.com", "cliente123");
    }

    /** Cria um usuario VENDEDOR (pelo ADMIN) e devolve o token dele. */
    protected String tokenVendedor() throws Exception {
        mvc.perform(json(post("/api/usuarios"), tokenAdmin(), """
                        {"nome": "Vendedor IT", "email": "vendedor.it@teste.com", "senha": "vendedor123", "perfil": "VENDEDOR"}"""))
                .andExpect(status().isCreated());
        return login("vendedor.it@teste.com", "vendedor123");
    }

    protected String corpo(MockHttpServletRequestBuilder req) throws Exception {
        return mvc.perform(req).andReturn().getResponse().getContentAsString();
    }

    /** Id de um dos livros de exemplo (migracao V3) pelo ISBN ficticio 999-00-0000-NNN. */
    protected Integer livroExemplo(String token, int numero) throws Exception {
        String isbn = "999-00-0000-%03d".formatted(numero);
        return JsonPath.read(corpo(comToken(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/livros").param("q", isbn), token)), "$[0].id");
    }

    /** Id do registro de estoque de um livro. */
    protected Integer estoqueDoLivro(String token, Integer livroId) throws Exception {
        java.util.List<Integer> ids = JsonPath.read(corpo(comToken(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/estoque"), token)), "$[?(@.livroId == " + livroId + ")].id");
        return ids.get(0);
    }

    protected static MockHttpServletRequestBuilder comToken(MockHttpServletRequestBuilder req, String token) {
        return req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    protected static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, String token, String body) {
        return comToken(req, token).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    protected static Integer id(String json) {
        return JsonPath.read(json, "$.id");
    }
}
