package com.example.livraria.dto;

import com.example.livraria.domain.Auditoria;

import java.time.LocalDateTime;

public record AuditoriaResponse(Long id, String tabela, String operacao, String usuario,
                                LocalDateTime dataEvento, String detalhes) {
    public static AuditoriaResponse from(Auditoria a) {
        return new AuditoriaResponse(a.getId(), a.getTabela(), a.getOperacao(), a.getUsuario(),
                a.getDataEvento(), a.getDetalhes());
    }
}
