package com.example.livraria.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "autor")
@Getter
@Setter
@NoArgsConstructor
public class Autor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_autor")
    private Integer id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 100)
    private String nacionalidade;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(columnDefinition = "text")
    private String biografia;

    /** Preenchida quando ha foto; tambem serve de versao na URL da imagem. */
    @Column(name = "foto_atualizada_em")
    private java.time.LocalDateTime fotoAtualizadaEm;
}
