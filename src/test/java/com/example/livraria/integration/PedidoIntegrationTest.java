package com.example.livraria.integration;

import com.example.livraria.DadosTeste;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fluxo de vendas de ponta a ponta pela API: estoque, pedido, pagamentos e status. */
class PedidoIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager em;

    /** Preparacao: define o saldo direto no banco (pela API a quantidade so muda por entradas e vendas). */
    private void definirEstoque(String token, Integer livroId, int quantidade) {
        em.flush();
        jdbc.update("UPDATE livraria.estoque SET quantidade = ?, estoque_minimo = 2 WHERE id_livro = ?", quantidade, livroId);
        em.clear(); // as proximas requisicoes leem o saldo novo, nao o que estava em cache
    }

    private int quantidadeEmEstoque(String token, Integer livroId) throws Exception {
        return JsonPath.read(corpo(comToken(get("/api/estoque/" + estoqueDoLivro(token, livroId)), token)), "$.quantidade");
    }

    private Integer criarCliente(String token, String nome) throws Exception {
        return id(mvc.perform(json(post("/api/clientes"), token, """
                        {"nome": "%s", "email": "%s", "cpf": "%s"}""".formatted(nome, DadosTeste.email("cli.it"), DadosTeste.cpf())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private String criarPedido(String token, Integer clienteId, Integer livroId, int qtd) throws Exception {
        return mvc.perform(json(post("/api/pedidos"), token, """
                        {"clienteId": %s, "itens": [{"livroId": %d, "quantidade": %d}]}""".formatted(clienteId, livroId, qtd)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("Venda completa: pedido baixa estoque, pagamentos quitam (PAGO), depois ENVIADO e ENTREGUE")
    void vendaCompleta() throws Exception {
        String vendedor = tokenVendedor();
        Integer livro = livroExemplo(vendedor, 11); // Harry Potter
        definirEstoque(vendedor, livro, 10);
        Integer cliente = criarCliente(vendedor, "Cliente Venda IT");

        String pedido = criarPedido(vendedor, cliente, livro, 3);
        Integer pedidoId = id(pedido);
        BigDecimal total = new BigDecimal(JsonPath.read(pedido, "$.valorTotal").toString());
        BigDecimal preco = new BigDecimal(JsonPath.read(pedido, "$.itens[0].valorUnitario").toString());
        org.assertj.core.api.Assertions.assertThat(total).isEqualByComparingTo(preco.multiply(BigDecimal.valueOf(3)));
        org.assertj.core.api.Assertions.assertThat(quantidadeEmEstoque(vendedor, livro)).isEqualTo(7);

        // pagamento parcial aprovado: pedido continua PENDENTE
        Integer pag1 = id(corpo(json(post("/api/pagamentos"), vendedor, """
                {"pedidoId": %d, "formaPagamento": "PIX", "valor": 1.00}""".formatted(pedidoId))));
        mvc.perform(json(put("/api/pagamentos/" + pag1 + "/status"), vendedor, "{\"status\": \"APROVADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoTransacao", startsWith("TX-")))
                .andExpect(jsonPath("$.statusPedido").value("PENDENTE"));

        // valor acima do saldo e recusado
        mvc.perform(json(post("/api/pagamentos"), vendedor, """
                        {"pedidoId": %d, "formaPagamento": "BOLETO", "valor": %s}""".formatted(pedidoId, total)))
                .andExpect(status().isConflict());

        // quita o saldo: pedido vira PAGO
        Integer pag2 = id(corpo(json(post("/api/pagamentos"), vendedor, """
                {"pedidoId": %d, "formaPagamento": "CARTAO_CREDITO", "valor": %s}""".formatted(pedidoId, total.subtract(BigDecimal.ONE)))));
        mvc.perform(json(put("/api/pagamentos/" + pag2 + "/status"), vendedor, "{\"status\": \"APROVADO\"}"))
                .andExpect(jsonPath("$.statusPedido").value("PAGO"));

        mvc.perform(comToken(get("/api/pedidos/" + pedidoId), vendedor))
                .andExpect(jsonPath("$.status").value("PAGO"))
                .andExpect(jsonPath("$.saldo").value(0.0))
                .andExpect(jsonPath("$.pagamentos", hasSize(2)))
                .andExpect(jsonPath("$.proximosStatus", contains("ENVIADO", "CANCELADO")));

        // envio e entrega
        mvc.perform(json(put("/api/pedidos/" + pedidoId + "/status"), vendedor, "{\"status\": \"ENVIADO\"}"))
                .andExpect(status().isOk());
        mvc.perform(json(put("/api/pedidos/" + pedidoId + "/status"), vendedor, "{\"status\": \"ENTREGUE\"}"))
                .andExpect(jsonPath("$.status").value("ENTREGUE"));

        // pedido entregue nao pode ser cancelado nem ter pagamento estornado
        mvc.perform(json(put("/api/pedidos/" + pedidoId + "/status"), vendedor, "{\"status\": \"CANCELADO\"}"))
                .andExpect(status().isConflict());
        mvc.perform(json(put("/api/pagamentos/" + pag2 + "/status"), vendedor, "{\"status\": \"ESTORNADO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Estoque insuficiente retorna 409 e nao altera o estoque")
    void estoqueInsuficiente() throws Exception {
        String admin = tokenAdmin();
        Integer livro = livroExemplo(admin, 12);
        definirEstoque(admin, livro, 1);
        Integer cliente = criarCliente(admin, "Cliente Sem Estoque IT");

        mvc.perform(json(post("/api/pedidos"), admin, """
                        {"clienteId": %d, "itens": [{"livroId": %d, "quantidade": 2}]}""".formatted(cliente, livro)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem", containsString("Estoque insuficiente")));
        org.assertj.core.api.Assertions.assertThat(quantidadeEmEstoque(admin, livro)).isEqualTo(1);
    }

    @Test
    @DisplayName("Cancelar pedido devolve o estoque; alterar itens ajusta o estoque")
    void cancelarEAlterar() throws Exception {
        String admin = tokenAdmin();
        Integer livroA = livroExemplo(admin, 21);
        Integer livroB = livroExemplo(admin, 22);
        definirEstoque(admin, livroA, 5);
        definirEstoque(admin, livroB, 5);
        Integer cliente = criarCliente(admin, "Cliente Cancela IT");

        Integer pedidoId = id(criarPedido(admin, cliente, livroA, 2));
        org.assertj.core.api.Assertions.assertThat(quantidadeEmEstoque(admin, livroA)).isEqualTo(3);

        // troca o item: devolve A e baixa B
        mvc.perform(json(put("/api/pedidos/" + pedidoId), admin, """
                        {"clienteId": %d, "itens": [{"livroId": %d, "quantidade": 4}]}""".formatted(cliente, livroB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].livroId").value(livroB));
        org.assertj.core.api.Assertions.assertThat(quantidadeEmEstoque(admin, livroA)).isEqualTo(5);
        org.assertj.core.api.Assertions.assertThat(quantidadeEmEstoque(admin, livroB)).isEqualTo(1);

        mvc.perform(json(put("/api/pedidos/" + pedidoId + "/status"), admin, "{\"status\": \"CANCELADO\"}"))
                .andExpect(jsonPath("$.status").value("CANCELADO"));
        org.assertj.core.api.Assertions.assertThat(quantidadeEmEstoque(admin, livroB)).isEqualTo(5);

        // cancelado nao aceita mais alteracao de itens; pode ser excluido (sem pagamentos)
        mvc.perform(json(put("/api/pedidos/" + pedidoId), admin, """
                        {"clienteId": %d, "itens": [{"livroId": %d, "quantidade": 1}]}""".formatted(cliente, livroB)))
                .andExpect(status().isConflict());
        mvc.perform(comToken(delete("/api/pedidos/" + pedidoId), admin)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Pedido sem itens ou com quantidade zero retorna 400")
    void validacao() throws Exception {
        mvc.perform(json(post("/api/pedidos"), tokenAdmin(), """
                        {"clienteId": 1, "itens": []}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.itens").exists());
        mvc.perform(json(post("/api/pedidos"), tokenAdmin(), """
                        {"clienteId": 1, "itens": [{"livroId": 1, "quantidade": 0}]}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CLIENTE compra para si, ve apenas os proprios pedidos e so pode cancela-los")
    void cliente() throws Exception {
        String admin = tokenAdmin();
        Integer livro = livroExemplo(admin, 41);
        definirEstoque(admin, livro, 10);
        Integer outroCliente = criarCliente(admin, "Outro Cliente IT");
        Integer pedidoDeOutro = id(criarPedido(admin, outroCliente, livro, 1));

        String cliente = tokenCliente(); // auto-cadastro cria o cadastro de cliente vinculado
        String meu = criarPedido(cliente, outroCliente, livro, 2); // clienteId enviado e ignorado
        Integer meuId = id(meu);
        org.assertj.core.api.Assertions.assertThat((String) JsonPath.read(meu, "$.clienteNome")).isEqualTo("Cliente Teste");

        mvc.perform(comToken(get("/api/pedidos"), cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(meuId)));
        mvc.perform(comToken(get("/api/pedidos/" + pedidoDeOutro), cliente)).andExpect(status().isNotFound());

        // nao pode alterar itens, excluir, nem marcar como enviado
        mvc.perform(json(put("/api/pedidos/" + meuId), cliente, """
                        {"itens": [{"livroId": %d, "quantidade": 1}]}""".formatted(livro)))
                .andExpect(status().isForbidden());
        mvc.perform(comToken(delete("/api/pedidos/" + meuId), cliente)).andExpect(status().isForbidden());
        mvc.perform(json(put("/api/pedidos/" + meuId + "/status"), cliente, "{\"status\": \"ENVIADO\"}"))
                .andExpect(status().isConflict());

        // pode cancelar o proprio pedido pendente
        mvc.perform(json(put("/api/pedidos/" + meuId + "/status"), cliente, "{\"status\": \"CANCELADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    @Test
    @DisplayName("CLIENTE paga o proprio pedido pendente; nao paga pedido de outro cliente nem pedido ja pago")
    void clientePaga() throws Exception {
        String admin = tokenAdmin();
        Integer livro = livroExemplo(admin, 42);
        definirEstoque(admin, livro, 10);
        Integer pedidoDeOutro = id(criarPedido(admin, criarCliente(admin, "Outro Pagador IT"), livro, 1));

        String cliente = tokenCliente();
        Integer meuId = id(criarPedido(cliente, null, livro, 2));

        mvc.perform(json(post("/api/pedidos/" + pedidoDeOutro + "/pagamento"), cliente, "{\"formaPagamento\": \"PIX\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(json(post("/api/pedidos/" + meuId + "/pagamento"), cliente, "{}"))
                .andExpect(status().isBadRequest());

        mvc.perform(json(post("/api/pedidos/" + meuId + "/pagamento"), cliente, "{\"formaPagamento\": \"CARTAO_CREDITO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGO"))
                .andExpect(jsonPath("$.saldo").value(0))
                .andExpect(jsonPath("$.pagamentos[0].status").value("APROVADO"))
                .andExpect(jsonPath("$.pagamentos[0].formaPagamento").value("CARTAO_CREDITO"));

        mvc.perform(json(post("/api/pedidos/" + meuId + "/pagamento"), cliente, "{\"formaPagamento\": \"PIX\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Permissoes: CLIENTE sem acesso a estoque/clientes/pagamentos; VENDEDOR sem acesso a catalogo/usuarios/auditoria")
    void permissoes() throws Exception {
        String cliente = tokenCliente();
        mvc.perform(comToken(get("/api/estoque"), cliente)).andExpect(status().isForbidden());
        mvc.perform(comToken(get("/api/clientes"), cliente)).andExpect(status().isForbidden());
        mvc.perform(comToken(get("/api/pagamentos"), cliente)).andExpect(status().isForbidden());
        mvc.perform(comToken(get("/api/auditoria"), cliente)).andExpect(status().isForbidden());

        String vendedor = tokenVendedor();
        mvc.perform(comToken(get("/api/estoque"), vendedor)).andExpect(status().isOk());
        mvc.perform(comToken(get("/api/clientes"), vendedor)).andExpect(status().isOk());
        mvc.perform(comToken(get("/api/pagamentos"), vendedor)).andExpect(status().isOk());
        mvc.perform(comToken(get("/api/livros"), vendedor)).andExpect(status().isOk());
        mvc.perform(json(post("/api/editoras"), vendedor, "{\"nome\": \"X\"}")).andExpect(status().isForbidden());
        mvc.perform(comToken(get("/api/usuarios"), vendedor)).andExpect(status().isForbidden());
        mvc.perform(comToken(get("/api/auditoria"), vendedor)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Filtro por status na listagem de pedidos e pagamentos")
    void filtros() throws Exception {
        String admin = tokenAdmin();
        Integer livro = livroExemplo(admin, 31);
        definirEstoque(admin, livro, 10);
        Integer cliente = criarCliente(admin, "Cliente Filtro IT");
        Integer pedidoId = id(criarPedido(admin, cliente, livro, 1));

        mvc.perform(comToken(get("/api/pedidos").param("status", "PENDENTE").param("q", "Filtro IT"), admin))
                .andExpect(jsonPath("$[*].id", hasItem(pedidoId)));
        mvc.perform(comToken(get("/api/pedidos").param("status", "ENTREGUE").param("q", "Filtro IT"), admin))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(comToken(get("/api/pagamentos").param("status", "APROVADO"), admin))
                .andExpect(status().isOk());
    }
}
