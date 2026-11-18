package com.example.livraria.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * As restricoes do banco protegem os dados mesmo se alguem contornar a aplicacao
 * (SQL direto). Cada comando abaixo deve ser recusado pelo PostgreSQL.
 */
class RestricoesBancoIntegrationTest extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
            // CHECK
            "UPDATE livraria.livro SET preco = 0 WHERE id_livro = (SELECT min(id_livro) FROM livraria.livro)",
            "UPDATE livraria.livro SET ano_publicacao = 3000 WHERE id_livro = (SELECT min(id_livro) FROM livraria.livro)",
            "UPDATE livraria.estoque SET quantidade = -1 WHERE id_estoque = (SELECT min(id_estoque) FROM livraria.estoque)",
            "INSERT INTO livraria.cliente (nome, cpf) VALUES ('X', '12345678900')",
            "INSERT INTO livraria.cliente (nome, estado) VALUES ('X', 'sp')",
            "INSERT INTO livraria.cliente (nome, cep) VALUES ('X', '1234')",
            "INSERT INTO livraria.editora (nome, cnpj) VALUES ('X restricao', '123')",
            "UPDATE livraria.usuario SET email = 'ADMIN@LIVRARIA.COM' WHERE email = 'admin@livraria.com'",
            "INSERT INTO livraria.pedido (id_cliente, status) VALUES ((SELECT min(id_cliente) FROM livraria.cliente), 'PERDIDO')",
            // NOT NULL / FK / UNIQUE
            "INSERT INTO livraria.cliente (email) VALUES ('sem-nome@teste.com')",
            "INSERT INTO livraria.livro (isbn, titulo, preco, id_categoria, id_editora) VALUES ('1', 'X', 10, 99999, 1)",
            "INSERT INTO livraria.livro (isbn, titulo, preco, id_categoria, id_editora) SELECT isbn, 'Duplicado', 10, 1, 1 FROM livraria.livro LIMIT 1",
            "INSERT INTO livraria.estoque (id_livro, quantidade) SELECT id_livro, 1 FROM livraria.estoque LIMIT 1"
    })
    @DisplayName("Comandos que violam restricoes do banco sao recusados")
    void recusa(String sql) {
        // garante que existe ao menos um cliente para os testes de pedido
        jdbc.update("INSERT INTO livraria.cliente (nome) VALUES ('Cliente Restricao IT')");
        assertThatThrownBy(() -> jdbc.update(sql)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Subtotal do item precisa ser quantidade x valor unitario")
    void subtotalCoerente() {
        jdbc.update("INSERT INTO livraria.cliente (nome) VALUES ('Cliente Subtotal IT')");
        Integer cliente = jdbc.queryForObject("SELECT max(id_cliente) FROM livraria.cliente", Integer.class);
        jdbc.update("INSERT INTO livraria.pedido (id_cliente) VALUES (?)", cliente);
        Integer pedido = jdbc.queryForObject("SELECT max(id_pedido) FROM livraria.pedido", Integer.class);
        Integer livro = jdbc.queryForObject("SELECT min(id_livro) FROM livraria.livro", Integer.class);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO livraria.item_pedido (id_pedido, id_livro, quantidade, valor_unitario, subtotal) VALUES (?, ?, 2, 10, 25)",
                pedido, livro)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
