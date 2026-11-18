package com.example.livraria.dto;

import com.example.livraria.domain.FormaPagamento;
import com.example.livraria.domain.Pagamento;
import com.example.livraria.domain.StatusPagamento;
import com.example.livraria.domain.StatusPedido;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PagamentoDtos {

    private PagamentoDtos() {
    }

    public record PagamentoRequest(
            @NotNull(message = "Informe o pedido") Integer pedidoId,
            @NotNull(message = "Informe a forma de pagamento") FormaPagamento formaPagamento,
            @NotNull(message = "Informe o valor")
            @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
            @Digits(integer = 10, fraction = 2, message = "Valor invalido")
            BigDecimal valor) {
    }

    public record StatusPagamentoRequest(@NotNull(message = "Informe o status") StatusPagamento status) {
    }

    public record PagamentoResponse(Integer id, Integer pedidoId, String clienteNome, StatusPedido statusPedido,
                                    BigDecimal valor, FormaPagamento formaPagamento, StatusPagamento status,
                                    LocalDateTime dataPagamento, String codigoTransacao) {
        public static PagamentoResponse from(Pagamento p) {
            return new PagamentoResponse(p.getId(), p.getPedido().getId(), p.getPedido().getCliente().getNome(),
                    p.getPedido().getStatus(), p.getValor(), p.getFormaPagamento(), p.getStatus(),
                    p.getDataPagamento(), p.getCodigoTransacao());
        }
    }
}
