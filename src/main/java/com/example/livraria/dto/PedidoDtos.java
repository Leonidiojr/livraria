package com.example.livraria.dto;

import com.example.livraria.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class PedidoDtos {

    private PedidoDtos() {
    }

    public record ItemRequest(
            @NotNull(message = "Informe o livro") Integer livroId,
            @NotNull(message = "Informe a quantidade") @Min(value = 1, message = "A quantidade deve ser no minimo 1") Integer quantidade) {
    }

    /** clienteId e ignorado quando quem faz o pedido e um usuario CLIENTE (usa o proprio cadastro). */
    public record PedidoRequest(
            Integer clienteId,
            @NotEmpty(message = "Inclua ao menos um item") List<@Valid ItemRequest> itens,
            @Size(max = 2000, message = "Observacoes devem ter no maximo 2000 caracteres") String observacoes) {
    }

    public record StatusRequest(@NotNull(message = "Informe o status") StatusPedido status) {
    }

    public record ItemResponse(Integer id, Integer livroId, String livroTitulo, Integer quantidade,
                               BigDecimal valorUnitario, BigDecimal subtotal, String capaUrl) {
        static ItemResponse from(ItemPedido i) {
            return new ItemResponse(i.getId(), i.getLivro().getId(), i.getLivro().getTitulo(), i.getQuantidade(),
                    i.getValorUnitario(), i.getSubtotal(),
                    com.example.livraria.service.ImagemService.url(
                            "/api/livros/" + i.getLivro().getId() + "/capa", i.getLivro().getCapaAtualizadaEm()));
        }
    }

    public record PedidoResponse(Integer id, Integer clienteId, String clienteNome, LocalDateTime dataPedido,
                                 StatusPedido status, BigDecimal valorTotal, BigDecimal valorPago, BigDecimal saldo,
                                 String observacoes, int quantidadeItens, List<ItemResponse> itens,
                                 List<PagamentoDtos.PagamentoResponse> pagamentos, List<StatusPedido> proximosStatus) {

        public static PedidoResponse from(Pedido p) {
            BigDecimal pago = p.totalPagamentos(StatusPagamento.APROVADO);
            return new PedidoResponse(p.getId(), p.getCliente().getId(), p.getCliente().getNome(), p.getDataPedido(),
                    p.getStatus(), p.getValorTotal(), pago, p.getValorTotal().subtract(pago), p.getObservacoes(),
                    p.getItens().stream().mapToInt(ItemPedido::getQuantidade).sum(),
                    p.getItens().stream().map(ItemResponse::from).toList(),
                    p.getPagamentos().stream().map(PagamentoDtos.PagamentoResponse::from).toList(),
                    p.getStatus().proximosManuais().stream().sorted().toList());
        }
    }

    public record PagarPedidoRequest(@NotNull(message = "Informe a forma de pagamento") FormaPagamento formaPagamento) {
    }
}
