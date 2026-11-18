package com.example.livraria.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/** Registro de auditoria gravado pelos triggers do banco (somente leitura na aplicacao). */
@Entity
@Immutable
@Table(name = "auditoria")
@Getter
public class Auditoria {

    @Id
    @Column(name = "id_auditoria")
    private Long id;

    @Column(nullable = false, length = 100)
    private String tabela;

    @Column(nullable = false, length = 20)
    private String operacao;

    @Column(name = "usuario_bd", length = 100)
    private String usuario;

    @Column(name = "data_evento")
    private LocalDateTime dataEvento;

    @Column(columnDefinition = "text")
    private String detalhes;
}
