package com.example.livraria;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/** Dados unicos para os testes: o sistema nao aceita dois clientes com o mesmo CPF ou e-mail. */
public final class DadosTeste {

    private static final AtomicLong SEQUENCIA = new AtomicLong(System.nanoTime() % 1_000_000L);

    private DadosTeste() {
    }

    /** CPF valido (digitos verificadores corretos) no formato 000.000.000-00, diferente a cada chamada. */
    public static String cpf() {
        int[] d = new int[11];
        do {
            long base = (SEQUENCIA.incrementAndGet() * 7919L + ThreadLocalRandom.current().nextInt(1000)) % 1_000_000_000L;
            for (int i = 8; i >= 0; i--) {
                d[i] = (int) (base % 10);
                base /= 10;
            }
        } while (todosIguais(d));
        d[9] = digito(d, 9);
        d[10] = digito(d, 10);
        return "%d%d%d.%d%d%d.%d%d%d-%d%d".formatted(d[0], d[1], d[2], d[3], d[4], d[5], d[6], d[7], d[8], d[9], d[10]);
    }

    /** E-mail unico com o prefixo informado. */
    public static String email(String prefixo) {
        return prefixo + "." + SEQUENCIA.incrementAndGet() + "@teste.com";
    }

    private static int digito(int[] d, int n) {
        int soma = 0;
        for (int i = 0; i < n; i++) {
            soma += d[i] * (n + 1 - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static boolean todosIguais(int[] d) {
        for (int i = 1; i < 9; i++) {
            if (d[i] != d[0]) {
                return false;
            }
        }
        return true;
    }
}
