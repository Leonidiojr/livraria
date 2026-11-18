import { api, sessao, ApiError } from "./api.js";

const $ = (s) => document.querySelector(s);
const formLogin = $("#form-login");
const formRegistro = $("#form-registro");
const msg = $("#mensagem");

function mostrar(texto, tipo = "erro") {
    msg.textContent = texto;
    msg.className = `alerta alerta-${tipo}`;
}
function esconderMsg() { msg.className = "alerta oculto"; }

if (sessao.token) window.location.replace("app.html");
if (new URLSearchParams(location.search).has("expirado")) mostrar("Sua sessao expirou. Entre novamente.");

$("#link-registro").addEventListener("click", (e) => {
    e.preventDefault(); esconderMsg();
    formLogin.classList.add("oculto"); formRegistro.classList.remove("oculto");
    $("#subtitulo").textContent = "Crie sua conta de cliente";
});
$("#link-login").addEventListener("click", (e) => {
    e.preventDefault(); esconderMsg();
    formRegistro.classList.add("oculto"); formLogin.classList.remove("oculto");
    $("#subtitulo").textContent = "Sistema de Vendas de Livros";
});

formLogin.addEventListener("submit", async (e) => {
    e.preventDefault();
    esconderMsg();
    const email = $("#login-email").value.trim();
    const senha = $("#login-senha").value;
    if (!email || !senha) return mostrar("Informe e-mail e senha.");

    const btn = $("#btn-entrar");
    btn.disabled = true;
    try {
        const r = await api("/auth/login", { method: "POST", body: { email, senha } });
        sessao.salvar(r.token, r.usuario);
        window.location.href = "app.html";
    } catch (err) {
        mostrar(err instanceof ApiError && err.status === 401 ? "E-mail ou senha invalidos." : err.message);
    } finally {
        btn.disabled = false;
    }
});

// mascara do CPF: 000.000.000-00
$("#reg-cpf").addEventListener("input", (e) => {
    const d = e.target.value.replace(/\D/g, "").slice(0, 11);
    e.target.value = d.replace(/^(\d{3})(\d)/, "$1.$2").replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
        .replace(/^(\d{3})\.(\d{3})\.(\d{3})(\d)/, "$1.$2.$3-$4");
});

formRegistro.addEventListener("submit", async (e) => {
    e.preventDefault();
    esconderMsg();
    formRegistro.querySelectorAll(".erro-campo").forEach((el) => (el.textContent = ""));
    const dados = Object.fromEntries(new FormData(formRegistro));
    try {
        await api("/auth/registro", { method: "POST", body: dados });
        formRegistro.reset();
        $("#link-login").click();
        $("#login-email").value = dados.email;
        mostrar("Conta criada! Entre com seu e-mail e senha.", "ok");
    } catch (err) {
        if (err instanceof ApiError) {
            Object.entries(err.campos).forEach(([campo, texto]) => {
                const el = formRegistro.querySelector(`[data-campo="${campo}"]`);
                if (el) el.textContent = texto;
            });
        }
        mostrar(err.message);
    }
});
