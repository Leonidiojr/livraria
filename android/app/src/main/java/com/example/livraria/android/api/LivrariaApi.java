package com.example.livraria.android.api;

import com.example.livraria.android.api.Modelos.Cliente;
import com.example.livraria.android.api.Modelos.Livro;
import com.example.livraria.android.api.Modelos.LoginRequest;
import com.example.livraria.android.api.Modelos.LoginResponse;
import com.example.livraria.android.api.Modelos.PagarRequest;
import com.example.livraria.android.api.Modelos.Pedido;
import com.example.livraria.android.api.Modelos.PedidoRequest;
import com.example.livraria.android.api.Modelos.RegistroRequest;
import com.example.livraria.android.api.Modelos.StatusRequest;
import com.example.livraria.android.api.Modelos.Usuario;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Endpoints da API do back-end Spring Boot usados pelo app. */
public interface LivrariaApi {

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest req);

    @POST("api/auth/registro")
    Call<Usuario> registrar(@Body RegistroRequest req);

    @GET("api/livros")
    Call<List<Livro>> livros(@Query("q") String busca);

    @GET("api/livros/{id}")
    Call<Livro> livro(@Path("id") int id);

    /** Somente ADMIN e VENDEDOR. */
    @GET("api/clientes")
    Call<List<Cliente>> clientes();

    @GET("api/pedidos")
    Call<List<Pedido>> pedidos();

    @GET("api/pedidos/{id}")
    Call<Pedido> pedido(@Path("id") int id);

    @POST("api/pedidos")
    Call<Pedido> criarPedido(@Body PedidoRequest req);

    @PUT("api/pedidos/{id}/status")
    Call<Pedido> alterarStatus(@Path("id") int id, @Body StatusRequest req);

    @POST("api/pedidos/{id}/pagamento")
    Call<Pedido> pagar(@Path("id") int id, @Body PagarRequest req);
}
