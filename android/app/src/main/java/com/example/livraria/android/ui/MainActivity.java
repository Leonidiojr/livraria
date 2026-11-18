package com.example.livraria.android.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.livraria.android.R;
import com.example.livraria.android.data.Carrinho;
import com.example.livraria.android.data.Sessao;
import com.example.livraria.android.databinding.ActivityMainBinding;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Tela principal com navegacao inferior: Catalogo, Carrinho e Pedidos. */
public class MainActivity extends AppCompatActivity implements Carrinho.Ouvinte {

    public static final String EXTRA_ABA = "aba";

    private ActivityMainBinding b;

    /** Abre a tela principal ja numa aba (ex.: R.id.nav_carrinho). */
    public static Intent abrirAba(Context context, int aba) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXTRA_ABA, aba);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Sessao sessao = Sessao.de(this);
        if (!sessao.logado()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        b.toolbar.setTitle(R.string.app_name);
        b.toolbar.setSubtitle("Olá, " + primeiroNome(sessao.nome()) + " · " + sessao.perfil());
        b.toolbar.inflateMenu(R.menu.principal);
        b.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.acao_sair) {
                confirmarSaida();
                return true;
            }
            return false;
        });

        b.navegacao.setOnItemSelectedListener(item -> {
            mostrar(item.getItemId());
            return true;
        });
        if (savedInstanceState == null) {
            b.navegacao.setSelectedItemId(getIntent().getIntExtra(EXTRA_ABA, R.id.nav_catalogo));
        }
        Carrinho.get().ouvir(this);
        carrinhoMudou();
        // Caso a permissao de rede local tenha sido revogada depois do login.
        com.example.livraria.android.util.RedeLocal.pedirSeNecessario(this);
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        if (b != null && intent.hasExtra(EXTRA_ABA)) {
            b.navegacao.setSelectedItemId(intent.getIntExtra(EXTRA_ABA, R.id.nav_catalogo));
        }
    }

    @Override
    protected void onDestroy() {
        Carrinho.get().pararDeOuvir(this);
        super.onDestroy();
    }

    public void irPara(int aba) {
        b.navegacao.setSelectedItemId(aba);
    }

    private void mostrar(int aba) {
        Fragment fragment;
        if (aba == R.id.nav_carrinho) {
            fragment = new CarrinhoFragment();
        } else if (aba == R.id.nav_pedidos) {
            fragment = new PedidosFragment();
        } else {
            fragment = new CatalogoFragment();
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.conteudo, fragment).commit();
    }

    @Override
    public void carrinhoMudou() {
        int quantidade = Carrinho.get().quantidadeTotal();
        if (quantidade == 0) {
            b.navegacao.removeBadge(R.id.nav_carrinho);
        } else {
            BadgeDrawable badge = b.navegacao.getOrCreateBadge(R.id.nav_carrinho);
            badge.setNumber(quantidade);
        }
    }

    private void confirmarSaida() {
        new MaterialAlertDialogBuilder(this)
                .setMessage("Deseja sair da sua conta?")
                .setPositiveButton(R.string.sair, (d, w) -> {
                    Sessao.de(this).sair();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private static String primeiroNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            return "";
        }
        return nome.trim().split("\\s+")[0];
    }
}
