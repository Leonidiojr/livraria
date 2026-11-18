package com.example.livraria.android.api;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.example.livraria.android.data.Sessao;
import com.example.livraria.android.ui.LoginActivity;
import com.google.gson.Gson;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Callback padrao das chamadas a API: entrega o corpo em {@link #sucesso}, traduz o JSON de erro
 * do servidor em mensagem legivel e, se o token expirou (401), volta para a tela de login.
 */
public abstract class Resposta<T> implements Callback<T> {

    private final Activity activity;

    protected Resposta(Activity activity) {
        this.activity = activity;
    }

    protected abstract void sucesso(T corpo);

    /** Activity que fez a chamada (segura de usar mesmo se o fragment ja saiu da tela). */
    protected Activity activity() {
        return activity;
    }

    /** Por padrao mostra a mensagem num Toast. */
    protected void falha(String mensagem) {
        Toast.makeText(activity, mensagem, Toast.LENGTH_LONG).show();
    }

    /** Chamado sempre ao final (sucesso ou falha), ex.: para esconder o carregamento. */
    protected void fim() {
    }

    @Override
    public final void onResponse(Call<T> call, Response<T> response) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        fim();
        if (response.isSuccessful()) {
            sucesso(response.body());
            return;
        }
        Sessao sessao = Sessao.de(activity);
        if (response.code() == 401 && sessao.token() != null) {
            sessao.sair();
            Toast.makeText(activity, "Sua sessão expirou. Entre novamente.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(activity, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
            return;
        }
        falha(mensagemDeErro(response));
    }

    @Override
    public final void onFailure(Call<T> call, Throwable t) {
        if (call.isCanceled() || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        fim();
        android.util.Log.w("Livraria", "Falha de rede em " + call.request().url(), t);
        falha("Não foi possível conectar ao servidor " + Sessao.de(activity).urlServidor()
                + ". Verifique se a API está rodando e o endereço nas configurações.");
    }

    static String mensagemDeErro(Response<?> response) {
        try (ResponseBody corpo = response.errorBody()) {
            if (corpo != null) {
                Modelos.Erro erro = new Gson().fromJson(corpo.charStream(), Modelos.Erro.class);
                if (erro != null && erro.mensagem != null) {
                    StringBuilder sb = new StringBuilder(erro.mensagem);
                    if (erro.campos != null) {
                        for (Map.Entry<String, String> campo : erro.campos.entrySet()) {
                            sb.append("\n• ").append(campo.getValue());
                        }
                    }
                    return sb.toString();
                }
            }
        } catch (RuntimeException ignorado) {
            // corpo nao e o JSON de erro esperado
        }
        switch (response.code()) {
            case 401:
                return "E-mail ou senha inválidos.";
            case 403:
                return "Seu perfil não tem permissão para esta ação.";
            case 404:
                return "Registro não encontrado.";
            default:
                return "Erro " + response.code() + " ao falar com o servidor.";
        }
    }
}
