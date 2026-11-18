package com.example.livraria.android.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.livraria.android.BuildConfig;
import com.example.livraria.android.api.Modelos.LoginResponse;

/** Guarda o token JWT, os dados do usuario logado e o endereco do servidor. */
public final class Sessao {

    public static final String ADMIN = "ADMIN";
    public static final String VENDEDOR = "VENDEDOR";
    public static final String CLIENTE = "CLIENTE";

    private static final String ARQUIVO = "sessao";
    private static Sessao instancia;

    private final SharedPreferences prefs;

    private Sessao(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    public static synchronized Sessao de(Context context) {
        if (instancia == null) {
            instancia = new Sessao(context);
        }
        return instancia;
    }

    public void entrar(LoginResponse resposta) {
        prefs.edit()
                .putString("token", resposta.token)
                .putString("nome", resposta.usuario != null ? resposta.usuario.nome : null)
                .putString("email", resposta.usuario != null ? resposta.usuario.email : null)
                .putString("perfil", resposta.usuario != null ? resposta.usuario.perfil : null)
                .apply();
    }

    public void sair() {
        prefs.edit().remove("token").remove("nome").remove("email").remove("perfil").apply();
        Carrinho.get().limpar();
    }

    public String token() {
        return prefs.getString("token", null);
    }

    public boolean logado() {
        return token() != null;
    }

    public String nome() {
        return prefs.getString("nome", "");
    }

    public String email() {
        return prefs.getString("email", "");
    }

    public String perfil() {
        return prefs.getString("perfil", CLIENTE);
    }

    public boolean ehCliente() {
        return CLIENTE.equals(perfil());
    }

    /** ADMIN ou VENDEDOR: vendem para qualquer cliente e mudam o status dos pedidos. */
    public boolean ehEquipe() {
        return ADMIN.equals(perfil()) || VENDEDOR.equals(perfil());
    }

    public String urlServidor() {
        return prefs.getString("url", BuildConfig.API_URL_PADRAO);
    }

    public void definirUrlServidor(String url) {
        String limpa = url.trim();
        if (!limpa.startsWith("http://") && !limpa.startsWith("https://")) {
            limpa = "http://" + limpa;
        }
        if (!limpa.endsWith("/")) {
            limpa += "/";
        }
        prefs.edit().putString("url", limpa).apply();
    }
}
