package com.example.livraria.android.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.livraria.android.R;
import com.example.livraria.android.api.ApiClient;
import com.example.livraria.android.api.Modelos.Pedido;
import com.example.livraria.android.api.Resposta;
import com.example.livraria.android.data.Sessao;
import com.example.livraria.android.databinding.FragmentListaBinding;
import com.example.livraria.android.databinding.ItemPedidoBinding;
import com.example.livraria.android.util.Formato;

import java.util.ArrayList;
import java.util.List;

/** Lista de pedidos: o CLIENTE ve so os proprios; ADMIN/VENDEDOR veem todos (regra aplicada pela API). */
public class PedidosFragment extends Fragment {

    private FragmentListaBinding b;
    private final PedidoAdapter adapter = new PedidoAdapter();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentListaBinding.inflate(inflater, container, false);
        adapter.mostrarCliente = Sessao.de(requireContext()).ehEquipe();
        b.lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.lista.setAdapter(adapter);
        b.atualizar.setColorSchemeResources(R.color.verde, R.color.dourado);
        b.atualizar.setOnRefreshListener(this::carregar);
        return b.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        carregar();
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }

    private void carregar() {
        if (b == null) {
            return;
        }
        b.atualizar.setRefreshing(true);
        ApiClient.api(requireContext()).pedidos().enqueue(new Resposta<List<Pedido>>(requireActivity()) {
            @Override
            protected void sucesso(List<Pedido> corpo) {
                if (b == null) {
                    return;
                }
                List<Pedido> lista = corpo == null ? new ArrayList<>() : corpo;
                adapter.definir(lista);
                b.vazio.setText(R.string.sem_pedidos);
                b.vazio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
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

    private static final class PedidoAdapter extends RecyclerView.Adapter<PedidoAdapter.Linha> {

        private final List<Pedido> pedidos = new ArrayList<>();
        boolean mostrarCliente;

        @SuppressWarnings("NotifyDataSetChanged")
        void definir(List<Pedido> novos) {
            pedidos.clear();
            pedidos.addAll(novos);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Linha onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Linha(ItemPedidoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Linha h, int position) {
            Pedido p = pedidos.get(position);
            h.b.numero.setText("Pedido #" + p.id);
            h.b.status.setText(Formato.statusPedido(p.status));
            h.b.cliente.setText(mostrarCliente
                    ? p.clienteNome + " · " + p.quantidadeItens + " item(ns)"
                    : p.quantidadeItens + " item(ns)");
            h.b.data.setText(Formato.dataHora(p.dataPedido));
            h.b.total.setText(Formato.moeda(p.valorTotal));
            h.itemView.setOnClickListener(v ->
                    v.getContext().startActivity(PedidoDetalheActivity.intent(v.getContext(), p.id, false)));
        }

        @Override
        public int getItemCount() {
            return pedidos.size();
        }

        static final class Linha extends RecyclerView.ViewHolder {
            final ItemPedidoBinding b;

            Linha(ItemPedidoBinding b) {
                super(b.getRoot());
                this.b = b;
            }
        }
    }
}
