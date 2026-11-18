// Cliente HTTP da API REST. Guarda o token JWT na sessionStorage.
const API_BASE = window.API_BASE || "/api";
const CHAVE_TOKEN = "livraria.token";
const CHAVE_USUARIO = "livraria.usuario";

export class ApiError extends Error {
    constructor(status, corpo) {
        super((corpo && corpo.mensagem) || `Erro HTTP ${status}`);
        this.status = status;
        this.campos = (corpo && corpo.campos) || {};
    }
}

export const sessao = {
    get token() { return sessionStorage.getItem(CHAVE_TOKEN); },
    get usuario() {
        try { return JSON.parse(sessionStorage.getItem(CHAVE_USUARIO)); } catch { return null; }
    },
    salvar(token, usuario) {
        sessionStorage.setItem(CHAVE_TOKEN, token);
        sessionStorage.setItem(CHAVE_USUARIO, JSON.stringify(usuario));
    },
    limpar() {
        sessionStorage.removeItem(CHAVE_TOKEN);
        sessionStorage.removeItem(CHAVE_USUARIO);
    },
    get isAdmin() { return this.usuario?.perfil === "ADMIN"; }
};

export async function api(caminho, { method = "GET", body } = {}) {
    const headers = { "Accept": "application/json" };
    if (body !== undefined) headers["Content-Type"] = "application/json";
    if (sessao.token) headers["Authorization"] = `Bearer ${sessao.token}`;

    const resp = await fetch(API_BASE + caminho, {
        method, headers, body: body !== undefined ? JSON.stringify(body) : undefined
    });
    return tratarResposta(caminho, resp);
}

/** Envia um arquivo (multipart, campo "arquivo"), usado para capas de livros e fotos de autores. */
export async function enviarArquivo(caminho, arquivo) {
    const dados = new FormData();
    dados.append("arquivo", arquivo);
    const headers = { "Accept": "application/json" };
    if (sessao.token) headers["Authorization"] = `Bearer ${sessao.token}`;
    const resp = await fetch(API_BASE + caminho, { method: "PUT", headers, body: dados });
    return tratarResposta(caminho, resp);
}

async function tratarResposta(caminho, resp) {
    if (resp.status === 401 && !caminho.startsWith("/auth/")) {
        sessao.limpar();
        window.location.href = "index.html?expirado=1";
        throw new ApiError(401, { mensagem: "Sessao expirada" });
    }
    if (resp.status === 204) return null;

    const texto = await resp.text();
    const corpo = texto ? JSON.parse(texto) : null;
    if (!resp.ok) {
        if (resp.status === 403) throw new ApiError(403, { mensagem: "Voce nao tem permissao para esta operacao" });
        throw new ApiError(resp.status, corpo);
    }
    return corpo;
}
