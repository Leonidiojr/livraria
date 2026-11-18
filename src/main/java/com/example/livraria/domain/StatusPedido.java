package com.example.livraria.domain;

import java.util.Map;
import java.util.Set;

/**
 * Ciclo de vida do pedido: PENDENTE -> PAGO -> ENVIADO -> ENTREGUE, com cancelamento possivel
 * enquanto PENDENTE ou PAGO. A passagem para PAGO acontece pela aprovacao dos pagamentos.
 */
public enum StatusPedido {
    PENDENTE, PAGO, ENVIADO, ENTREGUE, CANCELADO;

    private static final Map<StatusPedido, Set<StatusPedido>> TRANSICOES_MANUAIS = Map.of(
            PENDENTE, Set.of(CANCELADO),
            PAGO, Set.of(ENVIADO, CANCELADO),
            ENVIADO, Set.of(ENTREGUE),
            ENTREGUE, Set.of(),
            CANCELADO, Set.of());

    public boolean podeIrManualmentePara(StatusPedido destino) {
        return TRANSICOES_MANUAIS.get(this).contains(destino);
    }

    public Set<StatusPedido> proximosManuais() {
        return TRANSICOES_MANUAIS.get(this);
    }
}
