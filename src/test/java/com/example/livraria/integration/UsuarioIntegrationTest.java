package com.example.livraria.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuarioIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("CRUD completo de usuario pelo ADMIN, incluindo troca de senha")
    void crudCompleto() throws Exception {
        String token = tokenAdmin();

        String criado = mvc.perform(json(post("/api/usuarios"), token, """
                        {"nome": "Vendedor", "email": "Vendedor@Teste.com", "senha": "senha123", "perfil": "CLIENTE"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("vendedor@teste.com"))
                .andExpect(jsonPath("$.perfil").value("CLIENTE"))
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        Integer id = id(criado);

        mvc.perform(comToken(get("/api/usuarios").param("q", "vendedor"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(id)));

        mvc.perform(json(put("/api/usuarios/" + id), token, """
                        {"nome": "Vendedor Chefe", "email": "vendedor@teste.com", "senha": "novasenha", "perfil": "ADMIN", "ativo": true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Vendedor Chefe"))
                .andExpect(jsonPath("$.perfil").value("ADMIN"));

        // a senha nova funciona, a antiga nao
        login("vendedor@teste.com", "novasenha");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "vendedor@teste.com", "senha": "senha123"}"""))
                .andExpect(status().isUnauthorized());

        mvc.perform(comToken(delete("/api/usuarios/" + id), token)).andExpect(status().isNoContent());
        mvc.perform(comToken(get("/api/usuarios/" + id), token)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Usuario desativado nao consegue fazer login")
    void usuarioInativo() throws Exception {
        String token = tokenAdmin();
        mvc.perform(json(post("/api/usuarios"), token, """
                        {"nome": "Inativo", "email": "inativo@teste.com", "senha": "senha123", "perfil": "CLIENTE", "ativo": false}"""))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "inativo@teste.com", "senha": "senha123"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Regras: e-mail duplicado (409), senha obrigatoria na criacao (409), perfil invalido (409)")
    void regras() throws Exception {
        String token = tokenAdmin();
        mvc.perform(json(post("/api/usuarios"), token, """
                        {"nome": "Dup", "email": "admin@livraria.com", "senha": "senha123", "perfil": "CLIENTE"}"""))
                .andExpect(status().isConflict());
        mvc.perform(json(post("/api/usuarios"), token, """
                        {"nome": "Sem senha", "email": "semsenha@teste.com", "perfil": "CLIENTE"}"""))
                .andExpect(status().isConflict());
        mvc.perform(json(post("/api/usuarios"), token, """
                        {"nome": "Perfil", "email": "perfil@teste.com", "senha": "senha123", "perfil": "GERENTE"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Senha curta e e-mail invalido retornam 400")
    void validacao() throws Exception {
        mvc.perform(json(post("/api/usuarios"), tokenAdmin(), """
                        {"nome": "", "email": "x", "senha": "123", "perfil": ""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.senha").exists())
                .andExpect(jsonPath("$.campos.perfil").exists());
    }

    @Test
    @DisplayName("Administrador nao pode excluir a si mesmo")
    void naoExcluiASiMesmo() throws Exception {
        String token = tokenAdmin();
        String me = mvc.perform(comToken(get("/api/auth/me"), token))
                .andReturn().getResponse().getContentAsString();

        mvc.perform(comToken(delete("/api/usuarios/" + id(me)), token))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("CLIENTE nao acessa o CRUD de usuarios (403)")
    void clienteSemAcesso() throws Exception {
        String cliente = tokenCliente();
        mvc.perform(comToken(get("/api/usuarios"), cliente)).andExpect(status().isForbidden());
        mvc.perform(json(post("/api/usuarios"), cliente, """
                        {"nome": "X", "email": "x@teste.com", "senha": "senha123", "perfil": "ADMIN"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/perfis lista os perfis cadastrados")
    void perfis() throws Exception {
        mvc.perform(comToken(get("/api/perfis"), tokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItems("ADMIN", "CLIENTE")));
    }
}
