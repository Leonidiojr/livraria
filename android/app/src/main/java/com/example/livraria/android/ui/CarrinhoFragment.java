package com.example.livraria.android.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.livraria.android.R;
import com.example.livraria.android.api.ApiClient;
import com.example.livraria.android.api.Modelos.Cliente;
import com.example.livraria.android.api.Modelos.Pedido;
import com.example.livraria.android.api.Modelos.PedidoRequest;
import com.example.livraria.android.api.Resposta;
import com.example.livraria.android.data.Carrinho;
import com.example.livraria.android.data.Sessao;
import com.example.livraria.android.databinding.FragmentCarrinhoBinding;
import com.example.livraria.android.databinding.ItemCarrinhoBinding;
import com.example.livraria.android.util.Formato;

import java.util.ArrayList;
import java.util.List;

/**
 * Carrinho e finalizacao do pedido. O CLIENTE compra para si; ADMIN/VENDEDOR escolhem o cliente.
 * Depois de criar o pedido, abre a tela do pedido ja oferecendo o pagamento.
 */
public class CarrinhoFragment extends Fragment implements Carrinho.Ouvinte {

    private FragmentCarrinhoBinding b;
    private final ItemAdapter adapter = new ItemAdapter();
    private final List<Cliente> clientes = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentCarrinhoBinding.inflate(inflater, container, false);
        b.lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.lista.setAdapter(adapter);
        b.botaoFinalizar.setOnClickListener(v -> finalizar());
        if (Sessao.de(requireContext()).ehEquipe()) {
            b.blocoCliente.setVisibility(View.VISIBLE);
            carregarClientes();
        }
        Carrinho.get().ouvir(this);
        carrinhoMudou();
        return b.getRoot();
    }

    @Override
    public void onDestroyView() {
        Carrinho.get().pararDeOuvir(this);
        b = null;
        super.onDestroyView();
    }

    @Override
    public void carrinhoMudou() {
        if (b == null) {
            return;
        }
        Carrinho c = Carrinho.get();
        adapter.definir(c.itens());
        b.vazio.setVisibility(c.vazio() ? View.VISIBLE : View.GONE);
        b.rodape.setVisibility(c.vazio() ? View.GONE : View.VISIBLE);
        b.total.setText("Total: " + Formato.moeda(c.total()));
    }

    private void carregarClientes() {
        ApiClient.api(requireContext()).clientes().enqueue(new Resposta<List<Cliente>>(requireActivity()) {
            @Override
            protected void sucesso(List<Cliente> corpo) {
                if (b == null) {
                    return;
                }
                clientes.clear();
                if (corpo != null) {
                    clientes.addAll(corpo);
                }
                ArrayAdapter<Cliente> opcoes = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, clientes);
                opcoes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                b.cliente.setAdapter(opcoes);
            }
        });
    }

    private void finalizar() {
        Carrinho carrinho = Carrinho.get();
        if (carrinho.vazio()) {
            return;
        }
        Integer clienteId = null;
        if (Sessao.de(requireContext()).ehEquipe()) {
            int pos = b.cliente.getSelectedItemPosition();
            if (pos < 0 || pos >= clientes.size()) {
                Toast.makeText(requireContext(), "Selecione o cliente do pedido", Toast.LENGTH_SHORT).show();
                return;
            }
            clienteId = clientes.get(pos).id;
        }
        String obs = b.observacoes.getText() == null ? "" : b.observacoes.getText().toString().trim();
        b.botaoFinalizar.setEnabled(false);
        PedidoRequest req = new PedidoRequest(clienteId, carrinho.paraPedido(), obs.isEmpty() ? null : obs);
        ApiClient.api(requireContext()).criarPedido(req).enqueue(new Resposta<Pedido>(requireActivity()) {
            @Override
            protected void sucesso(Pedido pedido) {
                Carrinho.get().limpar();
                if (b != null) {
                    b.observacoes.setText("");
                }
                android.app.Activity tela = activity();
                tela.startActivity(PedidoDetalheActivity.intent(tela, pedido.id, true));
                if (tela instanceof MainActivity) {
                    ((MainActivity) tela).irPara(R.id.nav_pedidos);
                }
            }

            @Override
            protected void fim() {
                if (b != null) {
                    b.botaoFinalizar.setEnabled(true);
                }
            }
        });
    }

    private final class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.Linha> {

        private final List<Carrinho.Item> itens = new ArrayList<>();

        @SuppressWarnings("NotifyDataSetChanged")
        void definir(List<Carrinho.Item> novos) {
            itens.clear();
            itens.addAll(novos);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Linha onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Linha(ItemCarrinhoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Linha h, int position) {
            Carrinho.Item item = itens.get(position);
            int livroId = item.livro.id;
            h.b.titulo.setText(item.livro.titulo);
            h.b.subtotal.setText(item.quantidade + " × " + Formato.moeda(item.livro.preco)
                    + " = " + Formato.moeda(item.subtotal()));
            h.b.quantidade.setText(String.valueOf(item.quantidade));
            Formato.capa(h.b.capa, item.livro.titulo, item.livro.capaUrl);
            h.b.menos.setOnClickListener(v -> Carrinho.get().alterarQuantidade(livroId, item.quantidade - 1));
            h.b.mais.setOnClickListener(v -> {
                if (!Carrinho.get().alterarQuantidade(livroId, item.quantidade + 1)) {
                    Toast.makeText(v.getContext(), "Quantidade máxima em estoque atingida", Toast.LENGTH_SHORT).show();
                }
            });
            h.b.remover.setOnClickListener(v -> Carrinho.get().remover(livroId));
            h.itemView.setOnClickListener(v ->
                    v.getContext().startActivity(LivroDetalheActivity.intent(v.getContext(), livroId)));
        }

        @Override
        public int getItemCount() {
            return itens.size();
        }

        final class Linha extends RecyclerView.ViewHolder {
            final ItemCarrinhoBinding b;

            Linha(ItemCarrinhoBinding b) {
                super(b.getRoot());
                this.b = b;
            }
        }
    }
}
