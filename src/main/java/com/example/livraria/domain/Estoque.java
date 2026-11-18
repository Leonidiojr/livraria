package com.example.livraria.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "estoque")
@Getter
@Setter
@NoArgsConstructor
public class Estoque {

    public static final int MINIMO_PADRAO = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estoque")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_livro", nullable = false, unique = true)
    private Livro livro;

    @Column(nullable = false)
    private Integer quantidade = 0;

    @Column(name = "estoque_minimo")
    private Integer estoqueMinimo = MINIMO_PADRAO;

    @Column(name = "ultima_atualizacao")
    private LocalDateTime ultimaAtualizacao;

    public Estoque(Livro livro, int quantidade, int estoqueMinimo) {
        this.livro = livro;
        this.quantidade = quantidade;
        this.estoqueMinimo = estoqueMinimo;
    }

    public boolean abaixoDoMinimo() {
        return quantidade < (estoqueMinimo == null ? 0 : estoqueMinimo);
    }

    @PrePersist
    @PreUpdate
    void tocar() {
        ultimaAtualizacao = LocalDateTime.now();
    }
}
