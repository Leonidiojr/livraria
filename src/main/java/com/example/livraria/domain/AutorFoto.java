package com.example.livraria.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Foto do autor (tabela propria para nao carregar os bytes nas listagens). */
@Entity
@Table(name = "autor_foto")
@Getter
@Setter
@NoArgsConstructor
public class AutorFoto {

    @Id
    @Column(name = "id_autor")
    private Integer autorId;

    @Column(nullable = false)
    private byte[] conteudo;

    @Column(nullable = false, length = 30)
    private String tipo;

    /** Nome original do arquivo enviado (informativo). */
    @Column(name = "nome_arquivo", length = 255)
    private String nomeArquivo;

    @Column(name = "tamanho_bytes", nullable = false)
    private Integer tamanhoBytes;

    @Column(name = "data_envio", nullable = false)
    private LocalDateTime dataEnvio;
}
