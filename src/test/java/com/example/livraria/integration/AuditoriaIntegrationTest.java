package com.example.livraria.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auditoria por trigger. Estes testes NAO rodam dentro da transacao de teste: cada requisicao
 * abre a propria transacao, como em producao, para que o usuario logado chegue ao trigger.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuditoriaIntegrationTest extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void limpar() {
        jdbc.update("DELETE FROM livraria.autor WHERE nome LIKE 'Auditado IT%'");
        jdbc.update("DELETE FROM livraria.usuario WHERE email = 'auditado.it@teste.com'");
        jdbc.update("DELETE FROM livraria.auditoria WHERE detalhes LIKE '%Auditado IT%' OR detalhes LIKE '%auditado.it@teste.com%'");
    }

    @Test
    @DisplayName("Inclusao, alteracao e exclusao ficam registradas com o e-mail do usuario logado")
    void registraOperacoes() throws Exception {
        String admin = tokenAdmin();
        Integer autor = id(corpo(json(post("/api/autores"), admin, "{\"nome\": \"Auditado IT\"}")));
        mvc.perform(json(put("/api/autores/" + autor), admin, "{\"nome\": \"Auditado IT 2\", \"nacionalidade\": \"Brasileira\"}"))
                .andExpect(status().isOk());
        mvc.perform(comToken(delete("/api/autores/" + autor), admin)).andExpect(status().isNoContent());

        mvc.perform(comToken(get("/api/auditoria").param("tabela", "autor").param("q", "Auditado IT"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].operacao", contains("DELETE", "UPDATE", "INSERT")))
                .andExpect(jsonPath("$[*].usuario", everyItem(is(ADMIN_EMAIL))))
                .andExpect(jsonPath("$[1].detalhes", containsString("\"antes\"")))
                .andExpect(jsonPath("$[1].detalhes", containsString("Brasileira")));

        mvc.perform(comToken(get("/api/auditoria/tabelas"), admin))
                .andExpect(jsonPath("$", hasItem("autor")));
    }

    @Test
    @DisplayName("Listagens sem nenhum filtro funcionam (auditoria, pedidos, pagamentos)")
    void listagensSemFiltro() throws Exception {
        String admin = tokenAdmin();
        mvc.perform(comToken(get("/api/auditoria"), admin)).andExpect(status().isOk());
        mvc.perform(comToken(get("/api/auditoria").param("tabela", "usuario"), admin)).andExpect(status().isOk());
        mvc.perform(comToken(get("/api/pedidos"), admin)).andExpect(status().isOk());
        mvc.perform(comToken(get("/api/pagamentos"), admin)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("A senha (hash) do usuario nunca e gravada na auditoria")
    void naoGravaSenha() throws Exception {
        String admin = tokenAdmin();
        mvc.perform(json(post("/api/usuarios"), admin, """
                        {"nome": "Auditado IT", "email": "auditado.it@teste.com", "senha": "segredo123", "perfil": "VENDEDOR"}"""))
                .andExpect(status().isCreated());

        mvc.perform(comToken(get("/api/auditoria").param("tabela", "usuario").param("q", "auditado.it@teste.com"), admin))
                .andExpect(jsonPath("$[0].operacao").value("INSERT"))
                .andExpect(jsonPath("$[0].detalhes", not(containsString("senha_hash"))))
                .andExpect(jsonPath("$[0].detalhes", not(containsString("$2a$"))));
    }

    @Test
    @DisplayName("O login fica registrado (ultimo acesso) com o e-mail de quem entrou")
    void registraLogin() throws Exception {
        String admin = tokenAdmin();
        mvc.perform(comToken(get("/api/auditoria").param("tabela", "usuario").param("operacao", "update").param("limite", "1"), admin))
                .andExpect(jsonPath("$[0].usuario").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$[0].detalhes", containsString("ultimo_acesso")));
    }
}
