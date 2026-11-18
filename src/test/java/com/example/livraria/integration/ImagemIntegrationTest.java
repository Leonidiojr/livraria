package com.example.livraria.integration;

import com.example.livraria.Imagens;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Upload, leitura e remocao da capa do livro e da foto do autor. */
class ImagemIntegrationTest extends IntegrationTestBase {

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    private static MockMultipartHttpServletRequestBuilder upload(String url, byte[] bytes, String token) {
        MockMultipartHttpServletRequestBuilder req = multipart(HttpMethod.PUT, url);
        req.file(new MockMultipartFile("arquivo", "imagem.png", "image/png", bytes));
        if (token != null) {
            req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return req;
    }

    @Test
    @DisplayName("Capa do livro: enviar, ler publicamente (com cache), trocar e remover")
    void capa() throws Exception {
        String admin = tokenAdmin();
        Integer livro = livroExemplo(admin, 1);
        byte[] png = Imagens.png(60, 90);

        mvc.perform(comToken(get("/api/livros/" + livro), admin))
                .andExpect(jsonPath("$.capaUrl").doesNotExist());
        mvc.perform(get("/api/livros/" + livro + "/capa")).andExpect(status().isNotFound());

        String r = mvc.perform(upload("/api/livros/" + livro + "/capa", png, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capaUrl", startsWith("/api/livros/" + livro + "/capa?v=")))
                .andReturn().getResponse().getContentAsString();
        String url1 = JsonPath.read(r, "$.capaUrl");

        // metadados gravados junto com a imagem: nome do arquivo, tamanho e data do envio
        em.flush();
        java.util.Map<String, Object> meta = jdbc.queryForMap(
                "SELECT nome_arquivo, tamanho_bytes, data_envio FROM livraria.livro_capa WHERE id_livro = ?", livro);
        org.assertj.core.api.Assertions.assertThat(meta.get("nome_arquivo")).isEqualTo("imagem.png");
        org.assertj.core.api.Assertions.assertThat(meta.get("tamanho_bytes")).isEqualTo(png.length);
        org.assertj.core.api.Assertions.assertThat(meta.get("data_envio")).isNotNull();

        // leitura sem token (usada em <img>), com o tipo detectado e cache
        mvc.perform(get(url1))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(png))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("max-age")));

        // a capa aparece na listagem e no item do pedido
        mvc.perform(comToken(get("/api/livros").param("q", "999-00-0000-001"), admin))
                .andExpect(jsonPath("$[0].capaUrl").value(url1));

        // trocar por JPEG gera nova URL (versao)
        Thread.sleep(5);
        String url2 = JsonPath.read(mvc.perform(upload("/api/livros/" + livro + "/capa", Imagens.jpeg(60, 90), admin))
                .andReturn().getResponse().getContentAsString(), "$.capaUrl");
        org.assertj.core.api.Assertions.assertThat(url2).isNotEqualTo(url1);
        mvc.perform(get(url2)).andExpect(content().contentType("image/jpeg"));

        // dados do livro continuam iguais apos alterar o cadastro
        mvc.perform(comToken(get("/api/livros/" + livro), admin)).andExpect(jsonPath("$.capaUrl").value(url2));

        mvc.perform(comToken(delete("/api/livros/" + livro + "/capa"), admin)).andExpect(status().isNoContent());
        mvc.perform(comToken(get("/api/livros/" + livro), admin)).andExpect(jsonPath("$.capaUrl").doesNotExist());
        mvc.perform(get("/api/livros/" + livro + "/capa")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Foto do autor: enviar, ler e remover; excluir o autor apaga a foto")
    void foto() throws Exception {
        String admin = tokenAdmin();
        Integer autor = id(corpo(json(post("/api/autores"), admin, "{\"nome\": \"Autor Com Foto IT\"}")));

        mvc.perform(upload("/api/autores/" + autor + "/foto", Imagens.png(80, 80), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoUrl", startsWith("/api/autores/" + autor + "/foto?v=")));
        mvc.perform(comToken(get("/api/autores").param("q", "Com Foto IT"), admin))
                .andExpect(jsonPath("$[0].fotoUrl", notNullValue()));
        mvc.perform(get("/api/autores/" + autor + "/foto")).andExpect(status().isOk());

        mvc.perform(comToken(delete("/api/autores/" + autor), admin)).andExpect(status().isNoContent());
        mvc.perform(get("/api/autores/" + autor + "/foto")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Arquivo que nao e imagem retorna 400; sem arquivo retorna 400")
    void arquivoInvalido() throws Exception {
        String admin = tokenAdmin();
        Integer livro = livroExemplo(admin, 2);

        mvc.perform(upload("/api/livros/" + livro + "/capa", "isto nao e imagem".getBytes(), admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.arquivo", containsString("JPG, PNG ou WEBP")));
        mvc.perform(multipart(HttpMethod.PUT, "/api/livros/" + livro + "/capa")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isBadRequest());
        mvc.perform(upload("/api/livros/999999/capa", Imagens.png(5, 5), admin))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Somente ADMIN envia ou remove imagens; sem login retorna 401")
    void permissoes() throws Exception {
        String admin = tokenAdmin();
        Integer livro = livroExemplo(admin, 3);

        mvc.perform(upload("/api/livros/" + livro + "/capa", Imagens.png(5, 5), null))
                .andExpect(status().isUnauthorized());
        String vendedor = tokenVendedor();
        mvc.perform(upload("/api/livros/" + livro + "/capa", Imagens.png(5, 5), vendedor))
                .andExpect(status().isForbidden());
        mvc.perform(comToken(delete("/api/livros/" + livro + "/capa"), vendedor))
                .andExpect(status().isForbidden());
    }
}
