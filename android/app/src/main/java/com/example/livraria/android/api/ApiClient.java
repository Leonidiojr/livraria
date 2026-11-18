package com.example.livraria.android.api;

import android.content.Context;

import com.example.livraria.android.data.Sessao;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Cria o cliente Retrofit e envia o token JWT ("Authorization: Bearer ...") em toda requisicao. */
public final class ApiClient {

    private static LivrariaApi api;
    private static String urlAtual;

    private ApiClient() {
    }

    public static synchronized LivrariaApi api(Context context) {
        Sessao sessao = Sessao.de(context);
        String url = sessao.urlServidor();
        if (api == null || !url.equals(urlAtual)) {
            OkHttpClient http = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        String token = sessao.token();
                        if (token == null) {
                            return chain.proceed(original);
                        }
                        return chain.proceed(original.newBuilder()
                                .header("Authorization", "Bearer " + token)
                                .build());
                    })
                    .build();
            api = new Retrofit.Builder()
                    .baseUrl(url)
                    .client(http)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(LivrariaApi.class);
            urlAtual = url;
        }
        return api;
    }

    /** Converte caminhos relativos devolvidos pela API (ex.: capaUrl "/api/livros/1/capa?v=...") em URL completa. */
    public static String urlAbsoluta(Context context, String caminho) {
        if (caminho == null || caminho.isEmpty()) {
            return null;
        }
        if (caminho.startsWith("http://") || caminho.startsWith("https://")) {
            return caminho;
        }
        String base = Sessao.de(context).urlServidor();
        return base.replaceAll("/+$", "") + (caminho.startsWith("/") ? caminho : "/" + caminho);
    }
}
