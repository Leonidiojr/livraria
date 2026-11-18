package com.example.livraria.android.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.livraria.android.R;
import com.example.livraria.android.api.ApiClient;
import com.example.livraria.android.api.Modelos.AutorResumo;
import com.example.livraria.android.api.Modelos.Livro;
import com.example.livraria.android.api.Resposta;
import com.example.livraria.android.databinding.FragmentListaBinding;
import com.example.livraria.android.databinding.ItemLivroBinding;
import com.example.livraria.android.util.Formato;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

/** Catalogo de livros com busca (titulo, autor, ISBN...) feita pela API (?q=). */
public class CatalogoFragment extends Fragment {

    private FragmentListaBinding b;
    private final LivroAdapter adapter = new LivroAdapter();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable buscaAtrasada = this::carregar;
    private Call<List<Livro>> chamada;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentListaBinding.inflate(inflater, container, false);
        b.campoBusca.setVisibility(View.VISIBLE);
        b.campoBusca.setHint(R.string.buscar_livros);
        b.lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.lista.setAdapter(adapter);
        b.atualizar.setColorSchemeResources(R.color.verde, R.color.dourado);
        b.atualizar.setOnRefreshListener(this::carregar);
        b.busca.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(buscaAtrasada);
                handler.postDelayed(buscaAtrasada, 400);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        b.busca.setOnEditorActionListener((v, acao, e) -> {
            if (acao == EditorInfo.IME_ACTION_SEARCH) {
                handler.removeCallbacks(buscaAtrasada);
                carregar();
                return true;
            }
            return false;
        });
        return b.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        carregar(); // atualiza estoque ao voltar de uma compra
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(buscaAtrasada);
        if (chamada != null) {
            chamada.cancel();
        }
        b = null;
        super.onDestroyView();
    }

    private void carregar() {
        if (b == null) {
            return;
        }
        if (chamada != null) {
            chamada.cancel();
        }
        String termo = b.busca.getText() == null ? "" : b.busca.getText().toString().trim();
        b.atualizar.setRefreshing(true);
        chamada = ApiClient.api(requireContext()).livros(termo.isEmpty() ? null : termo);
        chamada.enqueue(new Resposta<List<Livro>>(requireActivity()) {
            @Override
            protected void sucesso(List<Livro> corpo) {
                if (b == null) {
                    return;
                }
                List<Livro> ativos = new ArrayList<>();
                if (corpo != null) {
                    for (Livro l : corpo) {
                        if (!Boolean.FALSE.equals(l.ativo)) {
                            ativos.add(l);
                        }
                    }
                }
                adapter.definir(ativos);
                b.vazio.setText(R.string.sem_livros);
                b.vazio.setVisibility(ativos.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            protected void falha(String mensagem) {
                if (b == null) {
                    return;
                }
                b.vazio.setText(mensagem);
                b.vazio.setVisibility(View.VISIBLE);
            }

            @Override
            protected void fim() {
                if (b != null) {
                    b.atualizar.setRefreshing(false);
                }
            }
        });
    }

    static String nomesAutores(List<AutorResumo> autores) {
        if (autores == null || autores.isEmpty()) {
            return "Autor não informado";
        }
        StringBuilder sb = new StringBuilder();
        for (AutorResumo a : autores) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(a.nome);
        }
        return sb.toString();
    }

    private static final class LivroAdapter extends RecyclerView.Adapter<LivroAdapter.Linha> {

        private final List<Livro> livros = new ArrayList<>();

        @SuppressWarnings("NotifyDataSetChanged")
        void definir(List<Livro> novos) {
            livros.clear();
            livros.addAll(novos);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Linha onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Linha(ItemLivroBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Linha h, int position) {
            Livro l = livros.get(position);
            h.b.titulo.setText(l.titulo);
            h.b.autores.setText(nomesAutores(l.autores));
            h.b.categoria.setText(l.categoriaNome + " · " + l.editoraNome);
            h.b.preco.setText(Formato.moeda(l.preco));
            int estoque = l.estoque();
            h.b.estoque.setText(estoque > 0 ? estoque + " em estoque" : "Esgotado");
            h.b.estoque.setTextColor(ContextCompat.getColor(h.itemView.getContext(),
                    estoque > 0 ? R.color.texto_suave : R.color.vermelho));
            Formato.capa(h.b.capa, l.titulo, l.capaUrl);
            h.itemView.setOnClickListener(v ->
                    v.getContext().startActivity(LivroDetalheActivity.intent(v.getContext(), l.id)));
        }

        @Override
        public int getItemCount() {
            return livros.size();
        }

        static final class Linha extends RecyclerView.ViewHolder {
            final ItemLivroBinding b;

            Linha(ItemLivroBinding b) {
                super(b.getRoot());
                this.b = b;
            }
        }
    }
}
