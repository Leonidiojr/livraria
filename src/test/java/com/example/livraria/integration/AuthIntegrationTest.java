package com.example.livraria.integration;

import com.example.livraria.DadosTeste;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("POST /api/auth/login com credenciais validas retorna token JWT")
    void loginValido() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@livraria.com", "senha": "admin123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuario.perfil").value("ADMIN"))
                .andExpect(jsonPath("$.usuario.senhaHash").doesNotExist());
    }

    @Test
    @DisplayName("Login com senha errada retorna 401")
    void loginSenhaErrada() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@livraria.com", "senha": "errada"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha invalidos"));
    }

    @Test
    @DisplayName("Login com campos vazios retorna 400 com erros por campo")
    void loginInvalido() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nao-e-email", "senha": ""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.senha").exists());
    }

    @Test
    @DisplayName("Rotas protegidas sem token retornam 401")
    void semToken() throws Exception {
        mvc.perform(get("/api/livros")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token invalido e tratado como anonimo (401)")
    void tokenInvalido() throws Exception {
        mvc.perform(comToken(get("/api/livros"), "token.invalido.aqui"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Registro publico cria CLIENTE e permite login; e-mail duplicado retorna 409")
    void registro() throws Exception {
        String token = tokenCliente();

        mvc.perform(comToken(get("/api/auth/me"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("cliente.it@teste.com"))
                .andExpect(jsonPath("$.perfil").value("CLIENTE"));

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Outro", "email": "CLIENTE.IT@teste.com", "cpf": "%s", "senha": "123456"}"""
                                .formatted(DadosTeste.cpf())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Cliente ja possui cadastro ativo com o e-mail cliente.it@teste.com"));

        // sem CPF o cadastro e recusado
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Sem CPF", "email": "sem.cpf@teste.com", "senha": "123456"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cpf").exists());
    }

    @Test
    @DisplayName("Front-end estatico (tela de login) e publico")
    void telaLoginPublica() throws Exception {
        mvc.perform(get("/index.html")).andExpect(status().isOk());
    }
}
