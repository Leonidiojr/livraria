package com.example.livraria.dto;

import com.example.livraria.domain.Estoque;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class EstoqueDtos {

    private EstoqueDtos() {
    }

    /** Criacao (livroId obrigatorio) ou ajuste (livroId ignorado) de estoque. */
    public record EstoqueRequest(
            Integer livroId,
            @NotNull(message = "Informe a quantidade") @Min(value = 0, message = "A quantidade nao pode ser negativa") Integer quantidade,
            @Min(value = 0, message = "O estoque minimo nao pode ser negativo") Integer estoqueMinimo) {
    }

    /** Edicao do estoque: so o minimo pode ser alterado (a quantidade muda por entradas e vendas). */
    public record EstoqueMinimoRequest(
            @NotNull(message = "Informe o estoque minimo")
            @Min(value = 0, message = "O estoque minimo nao pode ser negativo") Integer estoqueMinimo) {
    }

    public record EstoqueResponse(Integer id, Integer livroId, String livroTitulo, String livroIsbn,
                                  Integer quantidade, Integer estoqueMinimo, boolean abaixoDoMinimo,
                                  LocalDateTime ultimaAtualizacao) {
        public static EstoqueResponse from(Estoque e) {
            return new EstoqueResponse(e.getId(), e.getLivro().getId(), e.getLivro().getTitulo(), e.getLivro().getIsbn(),
                    e.getQuantidade(), e.getEstoqueMinimo(), e.abaixoDoMinimo(), e.getUltimaAtualizacao());
        }
    }
}
