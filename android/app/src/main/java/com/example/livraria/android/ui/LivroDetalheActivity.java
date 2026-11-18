package com.example.livraria.android.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.livraria.android.R;
import com.example.livraria.android.api.ApiClient;
import com.example.livraria.android.api.Modelos.Livro;
import com.example.livraria.android.api.Resposta;
import com.example.livraria.android.data.Carrinho;
import com.example.livraria.android.databinding.ActivityLivroDetalheBinding;
import com.example.livraria.android.util.Formato;

/** Detalhes do livro: capa, preco, estoque, ficha tecnica e sinopse, com botoes de compra. */
public class LivroDetalheActivity extends AppCompatActivity {

    private static final String EXTRA_ID = "livroId";

    private ActivityLivroDetalheBinding b;

    public static Intent intent(Context context, int livroId) {
        return new Intent(context, LivroDetalheActivity.class).putExtra(EXTRA_ID, livroId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityLivroDetalheBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        b.toolbar.setTitle("Detalhes do livro");
        b.toolbar.setNavigationOnClickListener(v -> finish());

        int id = getIntent().getIntExtra(EXTRA_ID, -1);
        ApiClient.api(this).livro(id).enqueue(new Resposta<Livro>(this) {
            @Override
            protected void sucesso(Livro livro) {
                mostrar(livro);
            }

            @Override
            protected void falha(String mensagem) {
                super.falha(mensagem);
                finish();
            }

            @Override
            protected void fim() {
                b.progresso.setVisibility(View.GONE);
            }
        });
    }

    private void mostrar(Livro l) {
        b.conteudo.setVisibility(View.VISIBLE);
        Formato.capa(b.capa, l.titulo, l.capaUrl);
        b.titulo.setText(l.titulo);
        b.subtitulo.setText(l.subtitulo);
        b.subtitulo.setVisibility(vazio(l.subtitulo) ? View.GONE : View.VISIBLE);
        b.autores.setText(CatalogoFragment.nomesAutores(l.autores));
        b.preco.setText(Formato.moeda(l.preco));

        int estoque = l.estoque();
        b.estoque.setText(estoque > 0 ? estoque + " unidade(s) em estoque" : "Esgotado");
        b.estoque.setTextColor(ContextCompat.getColor(this, estoque > 0 ? R.color.texto_suave : R.color.vermelho));
        b.botaoComprar.setEnabled(estoque > 0);
        b.botaoCarrinho.setEnabled(estoque > 0);

        StringBuilder ficha = new StringBuilder();
        linha(ficha, "Categoria", l.categoriaNome);
        linha(ficha, "Editora", l.editoraNome);
        linha(ficha, "ISBN", l.isbn);
        linha(ficha, "Ano", l.anoPublicacao == null ? null : String.valueOf(l.anoPublicacao));
        linha(ficha, "Páginas", l.numeroPaginas == null ? null : String.valueOf(l.numeroPaginas));
        linha(ficha, "Idioma", l.idioma);
        b.ficha.setText(ficha.toString().trim());
        b.descricao.setText(vazio(l.descricao) ? "Sinopse não informada." : l.descricao);

        b.botaoCarrinho.setOnClickListener(v -> {
            if (adicionar(l)) {
                Toast.makeText(this, "Adicionado ao carrinho", Toast.LENGTH_SHORT).show();
            }
        });
        b.botaoComprar.setOnClickListener(v -> {
            boolean jaNoCarrinho = false;
            for (Carrinho.Item item : Carrinho.get().itens()) {
                if (item.livro.id.equals(l.id)) {
                    jaNoCarrinho = true;
                    break;
                }
            }
            if (jaNoCarrinho || adicionar(l)) {
                startActivity(MainActivity.abrirAba(this, R.id.nav_carrinho));
                finish();
            }
        });
    }

    private boolean adicionar(Livro l) {
        if (!Carrinho.get().adicionar(l)) {
            Toast.makeText(this, "Quantidade máxima em estoque atingida", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private static void linha(StringBuilder sb, String rotulo, String valor) {
        if (!vazio(valor)) {
            sb.append(rotulo).append(": ").append(valor).append('\n');
        }
    }

    private static boolean vazio(String s) {
        return s == null || s.trim().isEmpty();
    }
}
