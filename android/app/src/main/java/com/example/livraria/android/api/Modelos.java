package com.example.livraria.android.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Espelho dos DTOs da API REST (pacote com.example.livraria.dto do back-end).
 * Campos publicos para serem lidos/gravados pelo Gson. Datas chegam como texto ISO-8601.
 */
public final class Modelos {

    private Modelos() {
    }

    // ------------------------------------------------------------------ autenticacao

    public static class LoginRequest {
        public final String email;
        public final String senha;

        public LoginRequest(String email, String senha) {
            this.email = email;
            this.senha = senha;
        }
    }

    public static class RegistroRequest {
        public final String nome;
        public final String email;
        public final String cpf;
        public final String senha;

        public RegistroRequest(String nome, String email, String cpf, String senha) {
            this.nome = nome;
            this.email = email;
            this.cpf = cpf;
            this.senha = senha;
        }
    }

    public static class LoginResponse {
        public String token;
        public String tipo;
        public String expiraEm;
        public Usuario usuario;
    }

    public static class Usuario {
        public Integer id;
        public String nome;
        public String email;
        public Boolean ativo;
        public String perfil;
    }

    // ------------------------------------------------------------------ catalogo

    public static class AutorResumo {
        public Integer id;
        public String nome;
    }

    public static class Livro {
        public Integer id;
        public String isbn;
        public String titulo;
        public String subtitulo;
        public String descricao;
        public Integer anoPublicacao;
        public Integer numeroPaginas;
        public String idioma;
        public BigDecimal preco;
        public Integer categoriaId;
        public String categoriaNome;
        public Integer editoraId;
        public String editoraNome;
        public List<AutorResumo> autores;
        public Boolean ativo;
        public Integer quantidadeEstoque;
        public String capaUrl;

        public int estoque() {
            return quantidadeEstoque == null ? 0 : quantidadeEstoque;
        }
    }

    public static class Cliente {
        public Integer id;
        public String nome;
        public String email;
        public String cpf;

        @Override
        public String toString() {
            return cpf == null ? nome : nome + " — " + cpf;
        }
    }

    // ------------------------------------------------------------------ pedidos

    public static class ItemRequest {
        public final Integer livroId;
        public final Integer quantidade;

        public ItemRequest(Integer livroId, Integer quantidade) {
            this.livroId = livroId;
            this.quantidade = quantidade;
        }
    }

    public static class PedidoRequest {
        /** Ignorado pelo servidor quando quem compra e um usuario CLIENTE. */
        public final Integer clienteId;
        public final List<ItemRequest> itens;
        public final String observacoes;

        public PedidoRequest(Integer clienteId, List<ItemRequest> itens, String observacoes) {
            this.clienteId = clienteId;
            this.itens = itens;
            this.observacoes = observacoes;
        }
    }

    public static class ItemPedido {
        public Integer id;
        public Integer livroId;
        public String livroTitulo;
        public Integer quantidade;
        public BigDecimal valorUnitario;
        public BigDecimal subtotal;
        public String capaUrl;
    }

    public static class Pagamento {
        public Integer id;
        public Integer pedidoId;
        public BigDecimal valor;
        public String formaPagamento;
        public String status;
        public String dataPagamento;
        public String codigoTransacao;
    }

    public static class Pedido {
        public Integer id;
        public Integer clienteId;
        public String clienteNome;
        public String dataPedido;
        public String status;
        public BigDecimal valorTotal;
        public BigDecimal valorPago;
        public BigDecimal saldo;
        public String observacoes;
        public int quantidadeItens;
        public List<ItemPedido> itens;
        public List<Pagamento> pagamentos;
        public List<String> proximosStatus;
    }

    public static class StatusRequest {
        public final String status;

        public StatusRequest(String status) {
            this.status = status;
        }
    }

    public static class PagarRequest {
        public final String formaPagamento;

        public PagarRequest(String formaPagamento) {
            this.formaPagamento = formaPagamento;
        }
    }

    // ------------------------------------------------------------------ erros

    /** Formato de erro do GlobalExceptionHandler: {"status", "mensagem", "campos", "timestamp"}. */
    public static class Erro {
        public int status;
        public String mensagem;
        public Map<String, String> campos;
    }
}
