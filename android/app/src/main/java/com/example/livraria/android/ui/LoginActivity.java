package com.example.livraria.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.livraria.android.R;
import com.example.livraria.android.api.ApiClient;
import com.example.livraria.android.api.Modelos.LoginRequest;
import com.example.livraria.android.api.Modelos.LoginResponse;
import com.example.livraria.android.api.Modelos.RegistroRequest;
import com.example.livraria.android.api.Modelos.Usuario;
import com.example.livraria.android.api.Resposta;
import com.example.livraria.android.data.Sessao;
import com.example.livraria.android.databinding.ActivityLoginBinding;
import com.example.livraria.android.util.RedeLocal;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Tela de login (JWT) e auto-cadastro de cliente (nome, e-mail, CPF e senha). */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding b;
    private boolean modoCadastro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Sessao.de(this).logado()) {
            abrirPrincipal();
            return;
        }
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        b.botaoAcao.setOnClickListener(v -> enviar());
        b.botaoAlternar.setOnClickListener(v -> alternarModo());
        b.botaoServidor.setOnClickListener(v -> configurarServidor());
        b.senha.setOnEditorActionListener((v, acao, evento) -> {
            if (acao == EditorInfo.IME_ACTION_DONE) {
                enviar();
                return true;
            }
            return false;
        });
        b.cpf.addTextChangedListener(new MascaraCpf(b.cpf));
        RedeLocal.pedirSeNecessario(this);
    }

    @Override
    public void onRequestPermissionsResult(int codigo, @NonNull String[] permissoes, @NonNull int[] resultados) {
        super.onRequestPermissionsResult(codigo, permissoes, resultados);
        if (codigo == RedeLocal.CODIGO_PEDIDO && !RedeLocal.concedida(this)) {
            mostrarErro(MENSAGEM_SEM_REDE_LOCAL);
        }
    }

    private static final String MENSAGEM_SEM_REDE_LOCAL =
            "Permita o acesso à rede local para o app falar com o servidor da livraria.";

    private void alternarModo() {
        modoCadastro = !modoCadastro;
        int visivel = modoCadastro ? View.VISIBLE : View.GONE;
        b.campoNome.setVisibility(visivel);
        b.campoCpf.setVisibility(visivel);
        b.titulo.setText(modoCadastro ? R.string.criar_conta : R.string.entrar);
        b.botaoAcao.setText(modoCadastro ? R.string.criar_conta : R.string.entrar);
        b.botaoAlternar.setText(modoCadastro ? R.string.ja_tem_conta : R.string.nao_tem_conta);
        b.erro.setVisibility(View.GONE);
    }

    private void enviar() {
        String email = texto(b.email).toLowerCase();
        String senha = texto(b.senha);
        if (email.isEmpty() || senha.isEmpty()) {
            mostrarErro("Informe e-mail e senha.");
            return;
        }
        if (!RedeLocal.pedirSeNecessario(this)) {
            mostrarErro(MENSAGEM_SEM_REDE_LOCAL);
            return;
        }
        if (modoCadastro) {
            cadastrar(email, senha);
        } else {
            entrar(email, senha);
        }
    }

    private void entrar(String email, String senha) {
        carregando(true);
        ApiClient.api(this).login(new LoginRequest(email, senha)).enqueue(new Resposta<LoginResponse>(this) {
            @Override
            protected void sucesso(LoginResponse corpo) {
                Sessao.de(LoginActivity.this).entrar(corpo);
                abrirPrincipal();
            }

            @Override
            protected void falha(String mensagem) {
                mostrarErro(mensagem);
            }

            @Override
            protected void fim() {
                carregando(false);
            }
        });
    }

    private void cadastrar(String email, String senha) {
        String nome = texto(b.nome);
        String cpf = texto(b.cpf);
        if (nome.isEmpty() || cpf.isEmpty()) {
            mostrarErro("Preencha nome e CPF.");
            return;
        }
        carregando(true);
        ApiClient.api(this).registrar(new RegistroRequest(nome, email, cpf, senha)).enqueue(new Resposta<Usuario>(this) {
            @Override
            protected void sucesso(Usuario corpo) {
                Toast.makeText(LoginActivity.this, "Conta criada! Entrando…", Toast.LENGTH_SHORT).show();
                entrar(email, senha);
            }

            @Override
            protected void falha(String mensagem) {
                carregando(false);
                mostrarErro(mensagem);
            }
        });
    }

    private void configurarServidor() {
        EditText campo = new EditText(this);
        campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        campo.setText(Sessao.de(this).urlServidor());
        FrameLayout moldura = new FrameLayout(this);
        int margem = (int) (20 * getResources().getDisplayMetrics().density);
        moldura.setPadding(margem, margem / 2, margem, 0);
        moldura.addView(campo);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.servidor)
                .setMessage("Emulador: http://10.0.2.2:8080/\nCelular: http://<IP do computador>:8080/")
                .setView(moldura)
                .setPositiveButton(R.string.salvar, (d, w) -> Sessao.de(this).definirUrlServidor(campo.getText().toString()))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void abrirPrincipal() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void carregando(boolean sim) {
        b.progresso.setVisibility(sim ? View.VISIBLE : View.GONE);
        b.botaoAcao.setEnabled(!sim);
        b.botaoAlternar.setEnabled(!sim);
        if (sim) {
            b.erro.setVisibility(View.GONE);
        }
    }

    private void mostrarErro(String mensagem) {
        b.erro.setText(mensagem);
        b.erro.setVisibility(View.VISIBLE);
    }

    private static String texto(EditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
