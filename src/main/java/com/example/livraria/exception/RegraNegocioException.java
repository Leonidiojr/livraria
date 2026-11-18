package com.example.livraria.exception;

/** Violacao de regra de negocio (ex.: e-mail ou ISBN duplicado). Mapeada para HTTP 409. */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
