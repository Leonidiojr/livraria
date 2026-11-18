package com.example.livraria.exception;

/** Arquivo enviado nao e uma imagem aceita (tipo ou tamanho). Mapeada para HTTP 400. */
public class ArquivoInvalidoException extends RuntimeException {
    public ArquivoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
