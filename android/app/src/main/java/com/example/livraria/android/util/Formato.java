package com.example.livraria.android.util;

import android.view.View;

import com.bumptech.glide.Glide;
import com.example.livraria.android.api.ApiClient;
import com.example.livraria.android.databinding.ViewCapaBinding;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Formatacao de valores para exibicao (moeda, data, status) e carregamento de capas. */
public final class Formato {

    private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");

    private Formato() {
    }

    public static String moeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(BRASIL).format(valor == null ? BigDecimal.ZERO : valor);
    }

    /** "2026-11-18T14:05:09.123" -> "18/11/2026 14:05" (sem java.time, que exige API 26). */
    public static String dataHora(String iso) {
        if (iso == null || iso.length() < 10) {
            return "";
        }
        String data = iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4);
        if (iso.length() >= 16 && iso.charAt(10) == 'T') {
            data += " " + iso.substring(11, 16);
        }
        return data;
    }

    public static String iniciais(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return "?";
        }
        StringBuilder sb = new StringBuilder();
        for (String palavra : texto.trim().split("\\s+")) {
            if (!palavra.isEmpty() && Character.isLetterOrDigit(palavra.charAt(0))) {
                sb.append(Character.toUpperCase(palavra.charAt(0)));
            }
            if (sb.length() == 2) {
                break;
            }
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }

    public static String statusPedido(String status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case "PENDENTE":
                return "Pendente";
            case "PAGO":
                return "Pago";
            case "ENVIADO":
                return "Enviado";
            case "ENTREGUE":
                return "Entregue";
            case "CANCELADO":
                return "Cancelado";
            default:
                return status;
        }
    }

    public static String statusPagamento(String status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case "PENDENTE":
                return "Pendente";
            case "APROVADO":
                return "Aprovado";
            case "CANCELADO":
                return "Cancelado";
            case "ESTORNADO":
                return "Estornado";
            default:
                return status;
        }
    }

    public static final String[] FORMAS_PAGAMENTO = {"PIX", "CARTAO_CREDITO", "CARTAO_DEBITO", "BOLETO"};
    public static final String[] FORMAS_PAGAMENTO_TEXTO = {"PIX", "Cartão de crédito", "Cartão de débito", "Boleto"};

    public static String formaPagamento(String forma) {
        for (int i = 0; i < FORMAS_PAGAMENTO.length; i++) {
            if (FORMAS_PAGAMENTO[i].equals(forma)) {
                return FORMAS_PAGAMENTO_TEXTO[i];
            }
        }
        return forma == null ? "" : forma;
    }

    /** Mostra a capa do livro ou, sem imagem, as iniciais do titulo. */
    public static void capa(ViewCapaBinding capa, String titulo, String capaUrl) {
        capa.iniciais.setText(iniciais(titulo));
        String url = ApiClient.urlAbsoluta(capa.getRoot().getContext(), capaUrl);
        if (url == null) {
            Glide.with(capa.imagem).clear(capa.imagem);
            capa.imagem.setVisibility(View.GONE);
        } else {
            capa.imagem.setVisibility(View.VISIBLE);
            Glide.with(capa.imagem).load(url).into(capa.imagem);
        }
    }
}
