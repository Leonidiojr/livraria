package com.example.livraria.android.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.livraria.android.R;
import com.example.livraria.android.api.ApiClient;
import com.example.livraria.android.api.Modelos.ItemPedido;
import com.example.livraria.android.api.Modelos.PagarRequest;
import com.example.livraria.android.api.Modelos.Pagamento;
import com.example.livraria.android.api.Modelos.Pedido;
import com.example.livraria.android.api.Modelos.StatusRequest;
import com.example.livraria.android.api.Resposta;
import com.example.livraria.android.data.Sessao;
import com.example.livraria.android.databinding.ActivityPedidoDetalheBinding;
import com.example.livraria.android.databinding.ItemCarrinhoBinding;
import com.example.livraria.android.util.Formato;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.math.BigDecimal;

/**
 * Detalhe do pedido: itens, pagamentos e acoes permitidas.
 * - Pedido PENDENTE com saldo: botao Pagar (PIX, cartao ou boleto; aprovacao simulada pelo servidor).
 * - CLIENTE: pode cancelar o proprio pedido pendente.
 * - ADMIN/VENDEDOR: botoes para os proximos status (ENVIADO, ENTREGUE, CANCELADO).
 */
public class PedidoDetalheActivity extends AppCompatActivity {

    private static final String EXTRA_ID = "pedidoId";
    private static final String EXTRA_PAGAR = "pagar";

    private ActivityPedidoDetalheBinding b;
    private int pedidoId;
    private boolean oferecerPagamento;

    public static Intent intent(Context context, int pedidoId, boolean oferecerPagamento) {
        return new Intent(context, PedidoDetalheActivity.class)
                .putExtra(EXTRA_ID, pedidoId)
                .putExtra(EXTRA_PAGAR, oferecerPagamento);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityPedidoDetalheBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        b.toolbar.setNavigationOnClickListener(v -> finish());

        pedidoId = getIntent().getIntExtra(EXTRA_ID, -1);
        oferecerPagamento = savedInstanceState == null && getIntent().getBooleanExtra(EXTRA_PAGAR, false);
        b.toolbar.setTitle("Pedido #" + pedidoId);
        carregar();
    }

    private void carregar() {
        b.progresso.setVisibility(View.VISIBLE);
        ApiClient.api(this).pedido(pedidoId).enqueue(new Resposta<Pedido>(this) {
            @Override
            protected void sucesso(Pedido pedido) {
                mostrar(pedido);
                if (oferecerPagamento) {
                    oferecerPagamento = false;
                    if (podePagar(pedido)) {
                        escolherFormaPagamento(pedido);
                    }
                }
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

    private void mostrar(Pedido p) {
        b.conteudo.setVisibility(View.VISIBLE);
        b.status.setText(Formato.statusPedido(p.status));

        StringBuilder resumo = new StringBuilder();
        resumo.append("Cliente: ").append(p.clienteNome).append('\n')
                .append("Data: ").append(Formato.dataHora(p.dataPedido)).append('\n')
                .append("Total: ").append(Formato.moeda(p.valorTotal)).append('\n')
                .append("Pago: ").append(Formato.moeda(p.valorPago));
        if (!"CANCELADO".equals(p.status)) {
            // Pedido cancelado nao tem saldo a pagar (pagamentos sao cancelados/estornados).
            resumo.append('\n').append("Saldo em aberto: ").append(Formato.moeda(p.saldo));
        }
        if (p.observacoes != null && !p.observacoes.isEmpty()) {
            resumo.append("\nObservações: ").append(p.observacoes);
        }
        b.resumo.setText(resumo);

        b.itens.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        if (p.itens != null) {
            for (ItemPedido item : p.itens) {
                ItemCarrinhoBinding linha = ItemCarrinhoBinding.inflate(inflater, b.itens, false);
                linha.titulo.setText(item.livroTitulo);
                linha.subtotal.setText(item.quantidade + " × " + Formato.moeda(item.valorUnitario)
                        + " = " + Formato.moeda(item.subtotal));
                Formato.capa(linha.capa, item.livroTitulo, item.capaUrl);
                linha.menos.setVisibility(View.GONE);
                linha.mais.setVisibility(View.GONE);
                linha.quantidade.setVisibility(View.GONE);
                linha.remover.setVisibility(View.GONE);
                int livroId = item.livroId;
                linha.getRoot().setOnClickListener(v -> startActivity(LivroDetalheActivity.intent(this, livroId)));
                b.itens.addView(linha.getRoot());
            }
        }

        if (p.pagamentos == null || p.pagamentos.isEmpty()) {
            b.pagamentos.setText("Nenhum pagamento registrado.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (Pagamento pg : p.pagamentos) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append("• ").append(Formato.formaPagamento(pg.formaPagamento))
                        .append(" — ").append(Formato.moeda(pg.valor))
                        .append(" — ").append(Formato.statusPagamento(pg.status));
                if (pg.dataPagamento != null) {
                    sb.append(" (").append(Formato.dataHora(pg.dataPagamento)).append(')');
                }
            }
            b.pagamentos.setText(sb);
        }

        montarAcoes(p);
    }

    private void montarAcoes(Pedido p) {
        b.acoes.removeAllViews();
        Sessao sessao = Sessao.de(this);
        if (podePagar(p)) {
            adicionarBotao("Pagar " + Formato.moeda(p.saldo), false, v -> escolherFormaPagamento(p));
        }
        if (p.proximosStatus == null) {
            return;
        }
        for (String status : p.proximosStatus) {
            boolean permitido = sessao.ehEquipe()
                    || ("CANCELADO".equals(status) && "PENDENTE".equals(p.status));
            if (!permitido) {
                continue;
            }
            if ("CANCELADO".equals(status)) {
                adicionarBotao("Cancelar pedido", true, v -> confirmarCancelamento());
            } else {
                adicionarBotao("Marcar como " + Formato.statusPedido(status).toLowerCase(), false,
                        v -> alterarStatus(status));
            }
        }
    }

    private void adicionarBotao(String texto, boolean secundario, View.OnClickListener acao) {
        MaterialButton botao = secundario
                ? new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
                : new MaterialButton(this);
        botao.setText(texto);
        botao.setOnClickListener(acao);
        b.acoes.addView(botao);
    }

    private static boolean podePagar(Pedido p) {
        return "PENDENTE".equals(p.status) && p.saldo != null && p.saldo.compareTo(BigDecimal.ZERO) > 0;
    }

    private void escolherFormaPagamento(Pedido p) {
        final int[] escolha = {0};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Pagamento de " + Formato.moeda(p.saldo))
                .setSingleChoiceItems(Formato.FORMAS_PAGAMENTO_TEXTO, 0, (d, i) -> escolha[0] = i)
                .setPositiveButton(R.string.pagar, (d, w) -> pagar(Formato.FORMAS_PAGAMENTO[escolha[0]]))
                .setNegativeButton("Depois", null)
                .show();
    }

    private void pagar(String forma) {
        b.progresso.setVisibility(View.VISIBLE);
        ApiClient.api(this).pagar(pedidoId, new PagarRequest(forma)).enqueue(new Resposta<Pedido>(this) {
            @Override
            protected void sucesso(Pedido pedido) {
                Toast.makeText(PedidoDetalheActivity.this, "Pagamento aprovado!", Toast.LENGTH_LONG).show();
                mostrar(pedido);
            }

            @Override
            protected void fim() {
                b.progresso.setVisibility(View.GONE);
            }
        });
    }

    private void confirmarCancelamento() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancelar pedido")
                .setMessage("Os livros voltam ao estoque e pagamentos aprovados são estornados. Confirma?")
                .setPositiveButton("Cancelar pedido", (d, w) -> alterarStatus("CANCELADO"))
                .setNegativeButton("Voltar", null)
                .show();
    }

    private void alterarStatus(String status) {
        b.progresso.setVisibility(View.VISIBLE);
        ApiClient.api(this).alterarStatus(pedidoId, new StatusRequest(status)).enqueue(new Resposta<Pedido>(this) {
            @Override
            protected void sucesso(Pedido pedido) {
                Toast.makeText(PedidoDetalheActivity.this,
                        "Pedido " + Formato.statusPedido(pedido.status).toLowerCase(), Toast.LENGTH_SHORT).show();
                mostrar(pedido);
            }

            @Override
            protected void fim() {
                b.progresso.setVisibility(View.GONE);
            }
        });
    }
}
