package com.example.livraria.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "livro")
@Getter
@Setter
@NoArgsConstructor
public class Livro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_livro")
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(nullable = false, length = 250)
    private String titulo;

    @Column(length = 250)
    private String subtitulo;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(name = "ano_publicacao")
    private Integer anoPublicacao;

    @Column(name = "numero_paginas")
    private Integer numeroPaginas;

    @Column(length = 50)
    private String idioma;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_editora", nullable = false)
    private Editora editora;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "livro_autor",
            joinColumns = @JoinColumn(name = "id_livro"),
            inverseJoinColumns = @JoinColumn(name = "id_autor"))
    private Set<Autor> autores = new LinkedHashSet<>();

    /** Cada livro possui um registro de estoque, criado e removido junto com ele. */
    @OneToOne(mappedBy = "livro", cascade = CascadeType.ALL, orphanRemoval = true)
    private Estoque estoque;

    public int quantidadeEmEstoque() {
        return estoque == null ? 0 : estoque.getQuantidade();
    }

    private Boolean ativo = Boolean.TRUE;

    @Column(name = "data_cadastro", updatable = false)
    private LocalDateTime dataCadastro;

    /** Preenchida quando ha capa; tambem serve de versao na URL da imagem. */
    @Column(name = "capa_atualizada_em")
    private LocalDateTime capaAtualizadaEm;

    @PrePersist
    void prePersist() {
        if (dataCadastro == null) {
            dataCadastro = LocalDateTime.now();
        }
        if (ativo == null) {
            ativo = Boolean.TRUE;
        }
    }
}
