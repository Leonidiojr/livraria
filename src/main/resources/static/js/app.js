import { api, enviarArquivo, sessao, ApiError } from "./api.js";

const $ = (s, raiz = document) => raiz.querySelector(s);
const esc = (v) => String(v ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const moeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const dinheiro = (v) => `<span class="preco">${moeda.format(Number(v || 0))}</span>`;
const data = (iso) => (iso ? new Date(iso.length === 10 ? iso + "T00:00:00" : iso).toLocaleDateString("pt-BR") : "");
const dataHora = (iso) => (iso ? new Date(iso).toLocaleString("pt-BR") : "-");
const badge = (texto, classe = "") => `<span class="badge ${classe}">${esc(texto)}</span>`;
const ativoBadge = (v) => (v ? badge("Ativo") : badge("Inativo", "badge-off"));
const statusBadge = (s) => badge(s, `st-${s}`);
const rotuloForma = { PIX: "PIX", CARTAO_CREDITO: "Cartao de credito", CARTAO_DEBITO: "Cartao de debito", BOLETO: "Boleto" };
const capital = (s) => s[0].toUpperCase() + s.slice(1);
// iniciais das palavras principais (ignora artigos e preposicoes curtas): "A Batalha do Apocalipse" -> "BA"
const iniciais = (texto) => {
    const palavras = String(texto || "").split(/\s+/).filter(Boolean);
    const principais = palavras.filter((p) => p.length > 2);
    return esc((principais.length ? principais : palavras).slice(0, 2).map((p) => p[0]).join("").toUpperCase());
};
const capaHtml = (l, classe = "capa-mini") => l.capaUrl
    ? `<img class="${classe}" src="${esc(l.capaUrl)}" alt="Capa de ${esc(l.titulo)}" loading="lazy">`
    : `<div class="${classe} capa-vazia" role="img" aria-label="Sem capa"><span>${iniciais(l.titulo)}</span></div>`;
// capa que abre os detalhes do livro ao ser clicada
const capaClicavel = (l, classe = "capa-mini") => l.id == null ? capaHtml(l, classe)
    : `<button class="capa-botao" type="button" data-detalhe-livro="${l.id}" title="Ver detalhes de ${esc(l.titulo)}"
        aria-label="Ver detalhes de ${esc(l.titulo)}">${capaHtml(l, classe)}</button>`;
const fotoHtml = (a, classe = "foto-mini") => a.fotoUrl
    ? `<img class="${classe}" src="${esc(a.fotoUrl)}" alt="Foto de ${esc(a.nome)}" loading="lazy">`
    : `<div class="${classe} foto-vazia" role="img" aria-label="Sem foto"><span>${iniciais(a.nome)}</span></div>`;
const TIPOS_IMAGEM = ["image/jpeg", "image/png", "image/webp"];
const TAMANHO_MAXIMO = 2 * 1024 * 1024;

const ADMIN = "ADMIN", VENDEDOR = "VENDEDOR", CLIENTE = "CLIENTE";
const TODOS = [ADMIN, VENDEDOR, CLIENTE];
const EQUIPE = [ADMIN, VENDEDOR];
const perfil = () => sessao.usuario?.perfil;
const pode = (perfis) => perfis.includes(perfil());

const opcoes = async (caminho, texto = (o) => o.nome) =>
    (await api(caminho)).map((o) => ({ valor: o.id, texto: texto(o) }));

// ===========================================================================
// Definicao das telas
// ===========================================================================
const TELAS = {
    livros: {
        titulo: "Livros", singular: "livro", recurso: "/livros", ver: TODOS, editar: [ADMIN],
        colunas: [
            { rotulo: "Capa", valor: (l) => capaClicavel(l), classe: "col-imagem" },
            { rotulo: "Titulo", valor: (l) => `<strong>${esc(l.titulo)}</strong>${l.subtitulo ? `<br><small>${esc(l.subtitulo)}</small>` : ""}` },
            { rotulo: "Autor(es)", valor: (l) => l.autores.map((a) => linkLivros("autor", a.id, a.nome)).join(", ") },
            { rotulo: "ISBN", valor: (l) => esc(l.isbn), classe: "col-opcional" },
            { rotulo: "Categoria", valor: (l) => linkLivros("categoria", l.categoriaId, l.categoriaNome), classe: "col-opcional" },
            { rotulo: "Editora", valor: (l) => linkLivros("editora", l.editoraId, l.editoraNome), classe: "col-opcional" },
            { rotulo: "Preco", valor: (l) => dinheiro(l.preco) },
            { rotulo: "Estoque", valor: (l) => esc(l.quantidadeEstoque) },
            { rotulo: "Situacao", valor: (l) => ativoBadge(l.ativo) }
        ],
        campos: async (registro) => [
            { nome: "capa", rotulo: "Capa do livro", tipo: "imagem", caminho: "capa", url: registro?.capaUrl, formato: "capa", inteira: true },
            { nome: "isbn", rotulo: "ISBN", obrigatorio: true },
            { nome: "titulo", rotulo: "Titulo", obrigatorio: true },
            { nome: "subtitulo", rotulo: "Subtitulo", inteira: true },
            { nome: "preco", rotulo: "Preco (R$)", tipo: "number", passo: "0.01", obrigatorio: true },
            { nome: "anoPublicacao", rotulo: "Ano de publicacao", tipo: "number" },
            { nome: "numeroPaginas", rotulo: "Numero de paginas", tipo: "number" },
            { nome: "idioma", rotulo: "Idioma" },
            { nome: "categoriaId", rotulo: "Categoria", tipo: "select", opcoes: await opcoes("/categorias"), obrigatorio: true },
            { nome: "editoraId", rotulo: "Editora", tipo: "select", opcoes: await opcoes("/editoras"), obrigatorio: true },
            { nome: "autoresIds", rotulo: "Autores (Ctrl+clique para varios)", tipo: "multiselect", opcoes: await opcoes("/autores"), inteira: true },
            { nome: "descricao", rotulo: "Descricao", tipo: "textarea", inteira: true },
            { nome: "ativo", rotulo: "Ativo (disponivel para venda)", tipo: "checkbox", padrao: true }
        ],
        paraFormulario: (l) => ({ ...l, autoresIds: l.autores.map((a) => a.id) }),
        acoesExtras: (l) => (l.ativo && l.quantidadeEstoque > 0 && pode(TELAS.pedidos.criar) ? [{ acao: "comprar", texto: "Comprar" }] : []),
        aoAcao: (acao, id) => (acao === "comprar" ? comprarLivro(Number(id)) : null)
    },

    autores: {
        titulo: "Autores", singular: "autor", recurso: "/autores", ver: TODOS, editar: [ADMIN],
        colunas: [
            { rotulo: "Foto", valor: (a) => fotoHtml(a), classe: "col-imagem" },
            { rotulo: "Nome", valor: (a) => `<strong>${esc(a.nome)}</strong>` },
            { rotulo: "Nacionalidade", valor: (a) => esc(a.nacionalidade) },
            { rotulo: "Nascimento", valor: (a) => data(a.dataNascimento) },
            { rotulo: "Biografia", valor: (a) => esc((a.biografia || "").slice(0, 90)) + ((a.biografia || "").length > 90 ? "..." : ""), classe: "col-opcional" }
        ],
        aoClicarLinha: (a) => abrirLivrosDe("autor", a.id, a.nome),
        campos: async (registro) => [
            { nome: "foto", rotulo: "Foto do autor", tipo: "imagem", caminho: "foto", url: registro?.fotoUrl, formato: "foto", inteira: true },
            { nome: "nome", rotulo: "Nome", obrigatorio: true, inteira: true },
            { nome: "nacionalidade", rotulo: "Nacionalidade" },
            { nome: "dataNascimento", rotulo: "Data de nascimento", tipo: "date" },
            { nome: "biografia", rotulo: "Biografia", tipo: "textarea", inteira: true }
        ]
    },

    editoras: {
        titulo: "Editoras", singular: "editora", recurso: "/editoras", ver: TODOS, editar: [ADMIN],
        colunas: [
            { rotulo: "Nome", valor: (e) => `<strong>${esc(e.nome)}</strong>` },
            { rotulo: "CNPJ", valor: (e) => esc(e.cnpj), classe: "col-opcional" },
            { rotulo: "Telefone", valor: (e) => esc(e.telefone), classe: "col-opcional" },
            { rotulo: "E-mail", valor: (e) => esc(e.email) },
            { rotulo: "Site", valor: (e) => (e.site ? `<a href="${esc(e.site)}" target="_blank" rel="noopener">${esc(e.site)}</a>` : "") }
        ],
        aoClicarLinha: (e) => abrirLivrosDe("editora", e.id, e.nome),
        campos: async () => [
            { nome: "nome", rotulo: "Nome", obrigatorio: true, inteira: true },
            { nome: "cnpj", rotulo: "CNPJ (00.000.000/0000-00)" },
            { nome: "telefone", rotulo: "Telefone" },
            { nome: "email", rotulo: "E-mail", tipo: "email" },
            { nome: "site", rotulo: "Site" }
        ]
    },

    categorias: {
        titulo: "Categorias", singular: "categoria", recurso: "/categorias", ver: TODOS, editar: [ADMIN],
        colunas: [
            { rotulo: "Nome", valor: (c) => `<strong>${esc(c.nome)}</strong>` },
            { rotulo: "Descricao", valor: (c) => esc(c.descricao) },
            { rotulo: "Situacao", valor: (c) => ativoBadge(c.ativo) },
            { rotulo: "Cadastro", valor: (c) => data(c.dataCadastro?.slice(0, 10)), classe: "col-opcional" }
        ],
        aoClicarLinha: (c) => abrirLivrosDe("categoria", c.id, c.nome),
        campos: async () => [
            { nome: "nome", rotulo: "Nome", obrigatorio: true, inteira: true },
            { nome: "descricao", rotulo: "Descricao", tipo: "textarea", inteira: true },
            { nome: "ativo", rotulo: "Categoria ativa", tipo: "checkbox", padrao: true }
        ]
    },

    estoque: {
        titulo: "Estoque", singular: "estoque", recurso: "/estoque", ver: EQUIPE, editar: EQUIPE,
        textoNovo: "Entrada de estoque", tituloNovo: "Entrada de estoque",
        filtro: { parametro: "baixo", opcoes: [["", "Todos os livros"], ["true", "Abaixo do minimo"]] },
        colunas: [
            { rotulo: "Livro", valor: (e) => `<strong>${esc(e.livroTitulo)}</strong>` },
            { rotulo: "ISBN", valor: (e) => esc(e.livroIsbn), classe: "col-opcional" },
            { rotulo: "Quantidade", valor: (e) => esc(e.quantidade) },
            { rotulo: "Minimo", valor: (e) => esc(e.estoqueMinimo) },
            { rotulo: "Situacao", valor: (e) => (e.abaixoDoMinimo ? badge("Abaixo do minimo", "st-baixo") : badge("OK")) },
            { rotulo: "Atualizado", valor: (e) => dataHora(e.ultimaAtualizacao), classe: "col-opcional" }
        ],
        campos: async (registro) => {
            if (registro) {
                return [
                    { nome: "livro", rotulo: "Livro", tipo: "texto", valorFixo: registro.livroTitulo, inteira: true },
                    // a quantidade so muda por "Entrada de estoque", vendas e cancelamentos
                    { nome: "quantidade", rotulo: "Quantidade em estoque (use Entrada de estoque para adicionar)", tipo: "texto",
                      valorFixo: registro.quantidade },
                    { nome: "estoqueMinimo", rotulo: "Estoque minimo", tipo: "number", obrigatorio: true }
                ];
            }
            // entrada de estoque: qualquer livro; a quantidade e somada ao saldo atual
            const livros = await api("/livros");
            return [
                { nome: "livroId", rotulo: "Livro", tipo: "select", obrigatorio: true, inteira: true,
                  opcoes: livros.map((l) => ({ valor: l.id, texto: `${l.titulo} (ISBN ${l.isbn}) - em estoque: ${l.quantidadeEstoque ?? 0}` })) },
                { nome: "quantidade", rotulo: "Quantidade a adicionar", tipo: "number", obrigatorio: true, padrao: 1 },
                { nome: "estoqueMinimo", rotulo: "Novo estoque minimo (opcional)", tipo: "number" }
            ];
        }
    },

    clientes: {
        titulo: "Clientes", singular: "cliente", recurso: "/clientes", ver: EQUIPE, editar: EQUIPE,
        colunas: [
            { rotulo: "Nome", valor: (c) => `<strong>${esc(c.nome)}</strong>` },
            { rotulo: "E-mail", valor: (c) => esc(c.email) },
            { rotulo: "CPF", valor: (c) => esc(c.cpf), classe: "col-opcional" },
            { rotulo: "Telefone", valor: (c) => esc(c.telefone), classe: "col-opcional" },
            { rotulo: "Cidade/UF", valor: (c) => esc([c.cidade, c.estado].filter(Boolean).join("/")) },
            { rotulo: "Acesso", valor: (c) => (c.usuarioId ? badge("Com login") : ""), classe: "col-opcional" }
        ],
        campos: async () => [
            { nome: "nome", rotulo: "Nome", obrigatorio: true, inteira: true },
            { nome: "email", rotulo: "E-mail", tipo: "email", obrigatorio: true },
            { nome: "cpf", rotulo: "CPF (000.000.000-00)", obrigatorio: true, mascara: "cpf" },
            { nome: "telefone", rotulo: "Telefone" },
            { nome: "dataNascimento", rotulo: "Data de nascimento", tipo: "date" },
            { nome: "cep", rotulo: "CEP (00000-000)" },
            { nome: "endereco", rotulo: "Endereco" },
            { nome: "numero", rotulo: "Numero" },
            { nome: "complemento", rotulo: "Complemento" },
            { nome: "bairro", rotulo: "Bairro" },
            { nome: "cidade", rotulo: "Cidade" },
            { nome: "estado", rotulo: "UF" }
        ]
    },

    pedidos: {
        titulo: "Pedidos", singular: "pedido", recurso: "/pedidos", ver: TODOS, editar: EQUIPE, criar: TODOS,
        filtro: { parametro: "status", opcoes: [["", "Todos os status"], ["PENDENTE", "Pendente"], ["PAGO", "Pago"], ["ENVIADO", "Enviado"], ["ENTREGUE", "Entregue"], ["CANCELADO", "Cancelado"]] },
        colunas: [
            { rotulo: "Pedido", valor: (p) => `<strong>#${p.id}</strong>` },
            { rotulo: "Data", valor: (p) => dataHora(p.dataPedido) },
            { rotulo: "Cliente", valor: (p) => esc(p.clienteNome) },
            { rotulo: "Itens", valor: (p) => esc(p.quantidadeItens), classe: "col-opcional" },
            { rotulo: "Total", valor: (p) => dinheiro(p.valorTotal) },
            { rotulo: "Pago", valor: (p) => dinheiro(p.valorPago), classe: "col-opcional" },
            { rotulo: "Status", valor: (p) => statusBadge(p.status) }
        ],
        // a acao "editar" so aparece para pedidos pendentes; "excluir" para pendentes/cancelados sem pagamentos
        podeEditar: (p) => p.status === "PENDENTE",
        podeExcluir: (p) => (p.status === "PENDENTE" || p.status === "CANCELADO") && !p.pagamentos.length,
        acoesExtras: (p) => [
            ...(clientePodePagar(p) ? [{ acao: "pagar", texto: "Pagar", primario: true }] : []),
            { acao: "ver", texto: "Detalhes" }
        ],
        formulario: () => formularioPedido,
        // o cliente segue direto para o pagamento depois de criar o pedido
        aposSalvar: (p) => (clientePodePagar(p) ? (abrirPagamentoCliente(p), true) : false),
        aoAcao: async (acao, id) => {
            if (acao === "ver") return abrirDetalhePedido(id);
            if (acao === "pagar") {
                try { abrirPagamentoCliente(await api(`/pedidos/${id}`)); } catch (err) { mensagem(err.message); }
            }
        }
    },

    pagamentos: {
        titulo: "Pagamentos", singular: "pagamento", recurso: "/pagamentos", ver: EQUIPE, editar: [], criar: EQUIPE,
        filtro: { parametro: "status", opcoes: [["", "Todos os status"], ["PENDENTE", "Pendente"], ["APROVADO", "Aprovado"], ["CANCELADO", "Cancelado"], ["ESTORNADO", "Estornado"]] },
        colunas: [
            { rotulo: "#", valor: (p) => esc(p.id) },
            { rotulo: "Pedido", valor: (p) => `#${p.pedidoId}` },
            { rotulo: "Cliente", valor: (p) => esc(p.clienteNome) },
            { rotulo: "Forma", valor: (p) => esc(rotuloForma[p.formaPagamento]) },
            { rotulo: "Valor", valor: (p) => dinheiro(p.valor) },
            { rotulo: "Status", valor: (p) => statusBadge(p.status) },
            { rotulo: "Data", valor: (p) => dataHora(p.dataPagamento), classe: "col-opcional" },
            { rotulo: "Transacao", valor: (p) => esc(p.codigoTransacao), classe: "col-opcional" }
        ],
        acoesExtras: (p) => p.status === "PENDENTE"
            ? [{ acao: "APROVADO", texto: "Aprovar" }, { acao: "CANCELADO", texto: "Cancelar", perigo: true }]
            : p.status === "APROVADO" ? [{ acao: "ESTORNADO", texto: "Estornar", perigo: true }] : [],
        aoAcao: (acao, id) => alterarStatusPagamento(id, acao),
        campos: async () => {
            const pendentes = (await api("/pedidos?status=PENDENTE")).filter((p) => Number(p.saldo) > 0);
            return [
                { nome: "pedidoId", rotulo: "Pedido pendente", tipo: "select", obrigatorio: true, inteira: true,
                  opcoes: pendentes.map((p) => ({ valor: p.id, texto: `#${p.id} - ${p.clienteNome} - saldo ${moeda.format(p.saldo)}`, extra: p.saldo })) },
                { nome: "formaPagamento", rotulo: "Forma de pagamento", tipo: "select", obrigatorio: true, padrao: "PIX",
                  opcoes: Object.entries(rotuloForma).map(([valor, texto]) => ({ valor, texto })) },
                { nome: "valor", rotulo: "Valor (R$)", tipo: "number", passo: "0.01", obrigatorio: true }
            ];
        },
        aposAbrirFormulario: () => {
            // preenche o valor com o saldo do pedido escolhido
            const sel = $("#f-pedidoId");
            sel.addEventListener("change", () => {
                const opt = sel.selectedOptions[0];
                if (opt?.dataset.extra) $("#f-valor").value = Number(opt.dataset.extra).toFixed(2);
            });
        }
    },

    usuarios: {
        titulo: "Usuarios", singular: "usuario", recurso: "/usuarios", ver: [ADMIN], editar: [ADMIN],
        colunas: [
            { rotulo: "Nome", valor: (u) => `<strong>${esc(u.nome)}</strong>` },
            { rotulo: "E-mail", valor: (u) => esc(u.email) },
            { rotulo: "Perfil", valor: (u) => badge(u.perfil) },
            { rotulo: "Situacao", valor: (u) => ativoBadge(u.ativo) },
            { rotulo: "Ultimo acesso", valor: (u) => dataHora(u.ultimoAcesso), classe: "col-opcional" }
        ],
        campos: async (registro) => [
            { nome: "nome", rotulo: "Nome", obrigatorio: true },
            { nome: "email", rotulo: "E-mail", tipo: "email", obrigatorio: true },
            { nome: "senha", rotulo: registro ? "Nova senha (deixe em branco para manter)" : "Senha", tipo: "password", obrigatorio: !registro },
            { nome: "perfil", rotulo: "Perfil", tipo: "select", opcoes: (await api("/perfis")).map((p) => ({ valor: p, texto: p })), obrigatorio: true, padrao: CLIENTE },
            { nome: "ativo", rotulo: "Usuario ativo", tipo: "checkbox", padrao: true }
        ],
        paraFormulario: (u) => ({ ...u, senha: "" })
    },

    auditoria: {
        titulo: "Auditoria", singular: "registro", recurso: "/auditoria", ver: [ADMIN], editar: [], criar: [],
        filtro: { parametro: "tabela", opcoes: async () => [["", "Todas as tabelas"], ...(await api("/auditoria/tabelas")).map((t) => [t, t])] },
        filtro2: { parametro: "operacao", opcoes: [["", "Todas as operacoes"], ["INSERT", "Inclusao"], ["UPDATE", "Alteracao"], ["DELETE", "Exclusao"]] },
        colunas: [
            { rotulo: "Data/hora", valor: (a) => dataHora(a.dataEvento) },
            { rotulo: "Tabela", valor: (a) => esc(a.tabela) },
            { rotulo: "Operacao", valor: (a) => badge(a.operacao, `op-${a.operacao}`) },
            { rotulo: "Usuario", valor: (a) => esc(a.usuario) },
            { rotulo: "Resumo", valor: (a) => esc(resumoAuditoria(a)), classe: "col-opcional" }
        ],
        acoesExtras: () => [{ acao: "ver", texto: "Detalhes" }],
        aoAcao: (acao, id, item) => abrirDetalheAuditoria(item)
    }
};

// ===========================================================================
// Estado e utilitarios de interface
// ===========================================================================
let abaAtual = null;
let itensAtuais = [];
let registroEditando = null;
let camposAtuais = [];
let formAtual = null;   // formulario customizado (pedido) quando aberto
let salvarAtual = null; // funcao chamada pelo botao Salvar

function mensagem(texto) {
    const el = $("#mensagem");
    el.textContent = texto;
    el.className = texto ? "alerta alerta-erro" : "alerta oculto";
}

function toast(texto) {
    const t = $("#toast");
    t.textContent = texto;
    t.classList.remove("oculto");
    clearTimeout(toast.timer);
    toast.timer = setTimeout(() => t.classList.add("oculto"), 2500);
}

const podeEditarTela = (tela) => pode(tela.editar || []);
const podeCriarTela = (tela) => pode(tela.criar || tela.editar || []);

// ===========================================================================
// Listagem
// ===========================================================================
async function prepararFiltro(seletor, def) {
    const el = $(seletor);
    if (!def) { el.classList.add("oculto"); el.innerHTML = ""; return; }
    const lista = typeof def.opcoes === "function" ? await def.opcoes() : def.opcoes;
    el.innerHTML = lista.map(([v, t]) => `<option value="${esc(v)}">${esc(t)}</option>`).join("");
    el.classList.remove("oculto");
}

function montarUrl(tela) {
    const params = new URLSearchParams();
    const busca = $("#campo-busca").value.trim();
    if (busca) params.set("q", busca);
    if (tela.filtro && $("#filtro").value) params.set(tela.filtro.parametro, $("#filtro").value);
    if (tela.filtro2 && $("#filtro2").value) params.set(tela.filtro2.parametro, $("#filtro2").value);
    const qs = params.toString();
    return tela.recurso + (qs ? `?${qs}` : "");
}

function botoesDaLinha(tela, item) {
    const b = [];
    for (const extra of tela.acoesExtras ? tela.acoesExtras(item) : []) {
        b.push(`<button class="btn btn-sm ${extra.perigo ? "btn-danger" : ""} ${extra.primario ? "btn-primary" : ""}" type="button" data-acao="${extra.acao}" data-id="${item.id}">${extra.texto}</button>`);
    }
    if (podeEditarTela(tela)) {
        if (!tela.podeEditar || tela.podeEditar(item)) {
            b.push(`<button class="btn btn-sm" type="button" data-acao="editar" data-id="${item.id}">Editar</button>`);
        }
        if (!tela.podeExcluir || tela.podeExcluir(item)) {
            b.push(`<button class="btn btn-sm btn-danger" type="button" data-acao="excluir" data-id="${item.id}">Excluir</button>`);
        }
    }
    return b.join("");
}

async function carregar() {
    const tela = TELAS[abaAtual];
    mensagem("");
    const cab = tela.colunas.map((c) => `<th class="${c.classe || ""}">${c.rotulo}</th>`).join("");
    $("#tabela-cab").innerHTML = `<tr>${cab}<th></th></tr>`;
    const corpo = $("#tabela-corpo");
    corpo.innerHTML = `<tr><td class="vazio" colspan="99">Carregando...</td></tr>`;

    try {
        itensAtuais = await api(montarUrl(tela));
        if (!itensAtuais.length) {
            corpo.innerHTML = `<tr><td class="vazio" colspan="99">Nenhum ${tela.singular} encontrado.</td></tr>`;
            return;
        }
        corpo.innerHTML = itensAtuais.map((item) => `
            <tr data-id="${item.id}" ${tela.aoClicarLinha ? 'class="linha-clicavel" tabindex="0" title="Ver livros"' : ""}>
                ${tela.colunas.map((c) => `<td class="${c.classe || ""}">${c.valor(item)}</td>`).join("")}
                <td class="acoes">${botoesDaLinha(tela, item)}</td>
            </tr>`).join("");
    } catch (err) {
        corpo.innerHTML = "";
        mensagem(err.message);
    }
}

// ===========================================================================
// Modal generico
// ===========================================================================
function abrirModal({ titulo, html, salvar = null, acoes = "", largo = false, textoSalvar = "Salvar" }) {
    $("#modal-titulo").textContent = titulo;
    $("#modal-erro").classList.add("oculto");
    $("#modal-campos").innerHTML = html;
    $("#modal-acoes").innerHTML = acoes;
    $("#modal").classList.toggle("largo", largo);
    salvarAtual = salvar;
    $("#btn-salvar").classList.toggle("oculto", !salvar);
    $("#btn-salvar").textContent = textoSalvar;
    $("#btn-cancelar").textContent = salvar ? "Cancelar" : "Fechar";
    if (!$("#modal").open) $("#modal").showModal();
    $("#modal-campos").querySelector("input:not([type=hidden]):not([readonly]), select, textarea")?.focus();
}

function erroNoModal(err) {
    const form = $("#form-registro");
    if (err instanceof ApiError) {
        Object.entries(err.campos).forEach(([campo, texto]) => {
            const el = form.querySelector(`[data-campo="${campo}"]`);
            if (el) el.textContent = texto;
        });
    }
    const box = $("#modal-erro");
    box.textContent = err.message;
    box.classList.remove("oculto");
}

// ===========================================================================
// Formulario generico (criar / editar)
// ===========================================================================
function htmlCampo(c, valor) {
    const id = `f-${c.nome}`;
    const req = c.obrigatorio ? "required" : "";
    const rot = `${c.rotulo}${c.obrigatorio ? " *" : ""}`;
    let input;
    switch (c.tipo) {
        case "texto":
            input = `<input id="${id}" type="text" value="${esc(c.valorFixo)}" readonly>`;
            break;
        case "imagem": {
            const previa = c.url
                ? `<img src="${esc(c.url)}" alt="">`
                : `<span class="previa-vazia">Sem ${c.formato === "foto" ? "foto" : "capa"}</span>`;
            return `<div class="campo campo-imagem ${c.inteira ? "inteira" : ""}">
                <label for="${id}">${c.rotulo}</label>
                <div class="imagem-editor">
                    <div class="previa previa-${c.formato}" id="previa-${c.nome}">${previa}</div>
                    <div>
                        <input id="${id}" name="${c.nome}" type="file" accept="image/jpeg,image/png,image/webp">
                        <small class="dica">JPG, PNG ou WEBP, ate 2 MB.</small>
                        ${c.url ? `<label class="checkbox"><input id="${id}-remover" type="checkbox"> Remover imagem atual</label>` : ""}
                    </div>
                </div>
                <div class="erro-campo" data-campo="${c.nome}"></div>
            </div>`;
        }
        case "textarea":
            input = `<textarea id="${id}" name="${c.nome}" ${req}>${esc(valor)}</textarea>`;
            break;
        case "select":
            input = `<select id="${id}" name="${c.nome}" ${req}>
                <option value="">Selecione...</option>
                ${c.opcoes.map((o) => `<option value="${esc(o.valor)}" ${o.extra !== undefined ? `data-extra="${esc(o.extra)}"` : ""}
                    ${String(o.valor) === String(valor) ? "selected" : ""}>${esc(o.texto)}</option>`).join("")}
            </select>`;
            break;
        case "multiselect": {
            const sel = new Set((valor || []).map(String));
            input = `<select id="${id}" name="${c.nome}" multiple>
                ${c.opcoes.map((o) => `<option value="${esc(o.valor)}" ${sel.has(String(o.valor)) ? "selected" : ""}>${esc(o.texto)}</option>`).join("")}
            </select>`;
            break;
        }
        case "checkbox":
            return `<div class="campo ${c.inteira ? "inteira" : ""}">
                <label class="checkbox"><input id="${id}" name="${c.nome}" type="checkbox" ${valor ? "checked" : ""}> ${c.rotulo}</label>
            </div>`;
        default:
            input = `<input id="${id}" name="${c.nome}" type="${c.tipo || "text"}" ${c.passo ? `step="${c.passo}"` : ""}
                     value="${esc(valor)}" ${req} ${c.tipo === "password" ? 'autocomplete="new-password"' : ""}>`;
    }
    return `<div class="campo ${c.inteira ? "inteira" : ""}">
        <label for="${id}">${rot}</label>${input}
        <div class="erro-campo" data-campo="${c.nome}"></div>
    </div>`;
}

function lerCamposGenericos() {
    const form = $("#form-registro");
    const dados = {};
    for (const c of camposAtuais) {
        if (c.tipo === "texto" || c.tipo === "imagem") continue;
        const el = form.elements[c.nome];
        if (c.tipo === "checkbox") dados[c.nome] = el.checked;
        else if (c.tipo === "multiselect") dados[c.nome] = [...el.selectedOptions].map((o) => Number(o.value));
        else if (c.tipo === "number" || /Id$/.test(c.nome)) dados[c.nome] = el.value === "" ? null : Number(el.value);
        else dados[c.nome] = el.value.trim() === "" ? null : el.value.trim();
    }
    return dados;
}

function validarObrigatorios() {
    const form = $("#form-registro");
    let valido = true;
    for (const c of camposAtuais) {
        if (!c.obrigatorio || c.tipo === "texto" || c.tipo === "imagem") continue;
        if (!form.elements[c.nome].value) {
            form.querySelector(`[data-campo="${c.nome}"]`).textContent = "Campo obrigatorio";
            valido = false;
        }
    }
    return valido;
}

async function abrirFormulario(id, preset = null) {
    const tela = TELAS[abaAtual];
    try {
        registroEditando = id ? await api(`${tela.recurso}/${id}`) : null;
        if (tela.formulario) {
            formAtual = tela.formulario();
            camposAtuais = [];
            const html = await formAtual.html(registroEditando);
            abrirModal({ titulo: `${registroEditando ? "Editar" : "Novo"} ${tela.singular}`, html, salvar: salvarFormulario, largo: true });
            formAtual.aposAbrir?.(registroEditando ?? preset);
            return;
        }
        formAtual = null;
        camposAtuais = await tela.campos(registroEditando);
    } catch (err) {
        return mensagem(err.message);
    }
    const valores = registroEditando ? (tela.paraFormulario ? tela.paraFormulario(registroEditando) : registroEditando) : {};
    abrirModal({
        titulo: registroEditando ? `Editar ${tela.singular}` : (tela.tituloNovo || `Novo ${tela.singular}`),
        html: camposAtuais.map((c) => htmlCampo(c, registroEditando ? valores[c.nome] : c.padrao)).join(""),
        salvar: salvarFormulario
    });
    for (const c of camposAtuais.filter((c) => c.tipo === "imagem")) {
        $(`#f-${c.nome}`).addEventListener("change", (e) => {
            const arquivo = e.target.files[0];
            const erro = validarImagem(arquivo);
            $(`[data-campo="${c.nome}"]`).textContent = erro || "";
            if (arquivo && !erro) {
                $(`#previa-${c.nome}`).innerHTML = `<img src="${URL.createObjectURL(arquivo)}" alt="Pre-visualizacao">`;
                const remover = $(`#f-${c.nome}-remover`);
                if (remover) remover.checked = false;
            }
        });
    }
    for (const c of camposAtuais.filter((c) => c.mascara === "cpf")) {
        $(`#f-${c.nome}`).addEventListener("input", (e) => (e.target.value = mascaraCpf(e.target.value)));
    }
    tela.aposAbrirFormulario?.();
}

function mascaraCpf(valor) {
    const d = valor.replace(/\D/g, "").slice(0, 11);
    return d.replace(/^(\d{3})(\d)/, "$1.$2").replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
        .replace(/^(\d{3})\.(\d{3})\.(\d{3})(\d)/, "$1.$2.$3-$4");
}

function validarImagem(arquivo) {
    if (!arquivo) return null;
    if (!TIPOS_IMAGEM.includes(arquivo.type)) return "Formato nao suportado: use JPG, PNG ou WEBP";
    if (arquivo.size > TAMANHO_MAXIMO) return "A imagem deve ter no maximo 2 MB";
    return null;
}

async function salvarFormulario() {
    const tela = TELAS[abaAtual];
    const imagens = camposAtuais.filter((c) => c.tipo === "imagem");
    let imagensOk = true;
    for (const c of imagens) {
        const erro = validarImagem($(`#f-${c.nome}`).files[0]);
        if (erro) { $(`[data-campo="${c.nome}"]`).textContent = erro; imagensOk = false; }
    }
    const camposOk = formAtual ? true : validarObrigatorios();
    const corpo = formAtual ? formAtual.ler() : (camposOk && imagensOk ? lerCamposGenericos() : null);
    if (!corpo) return;
    const salvo = registroEditando
        ? await api(`${tela.recurso}/${registroEditando.id}`, { method: "PUT", body: corpo })
        : await api(tela.recurso, { method: "POST", body: corpo });
    // a partir daqui o registro existe: um novo "Salvar" (ex.: apos erro no upload) vira alteracao
    registroEditando = salvo;
    for (const c of imagens) {
        const arquivo = $(`#f-${c.nome}`).files[0];
        if (arquivo) {
            await enviarArquivo(`${tela.recurso}/${salvo.id}/${c.caminho}`, arquivo);
        } else if ($(`#f-${c.nome}-remover`)?.checked) {
            await api(`${tela.recurso}/${salvo.id}/${c.caminho}`, { method: "DELETE" });
        }
    }
    toast(`${capital(tela.singular)} salvo com sucesso`);
    await carregar();
    if (!tela.aposSalvar?.(salvo)) $("#modal").close();
}

async function aoSubmeter(e) {
    e.preventDefault();
    if (!salvarAtual) return;
    const form = $("#form-registro");
    form.querySelectorAll(".erro-campo").forEach((el) => (el.textContent = ""));
    $("#modal-erro").classList.add("oculto");
    const btn = $("#btn-salvar");
    btn.disabled = true;
    try {
        await salvarAtual();
    } catch (err) {
        erroNoModal(err);
    } finally {
        btn.disabled = false;
    }
}

async function excluir(id) {
    const tela = TELAS[abaAtual];
    if (!confirm(`Confirma a exclusao deste ${tela.singular}?`)) return;
    try {
        await api(`${tela.recurso}/${id}`, { method: "DELETE" });
        toast(`${capital(tela.singular)} excluido`);
        await carregar();
    } catch (err) {
        mensagem(err.message);
    }
}

// ===========================================================================
// Pedidos: formulario com itens e tela de detalhes
// ===========================================================================
const formularioPedido = {
    livros: [],

    async html(pedido) {
        const equipe = pode(EQUIPE);
        const [livros, clientes] = await Promise.all([api("/livros"), equipe ? api("/clientes") : Promise.resolve([])]);
        this.livros = livros.filter((l) => l.ativo);
        const clienteSel = equipe ? `
            <div class="campo inteira">
                <label for="f-clienteId">Cliente *</label>
                <select id="f-clienteId" name="clienteId" required>
                    <option value="">Selecione...</option>
                    ${clientes.map((c) => `<option value="${c.id}" ${pedido?.clienteId === c.id ? "selected" : ""}>${esc(c.nome)}${c.cpf ? ` - ${esc(c.cpf)}` : ""}</option>`).join("")}
                </select>
                <div class="erro-campo" data-campo="clienteId"></div>
            </div>` : "";
        return `${clienteSel}
            <div class="campo inteira">
                <label>Itens *</label>
                <div id="itens-pedido" class="itens-pedido"></div>
                <button class="btn btn-sm" type="button" id="btn-add-item">+ Adicionar item</button>
                <div class="erro-campo" data-campo="itens"></div>
                <div class="total-pedido">Total: <span id="total-pedido">${moeda.format(0)}</span></div>
            </div>
            <div class="campo inteira">
                <label for="f-observacoes">Observacoes</label>
                <textarea id="f-observacoes" name="observacoes">${esc(pedido?.observacoes)}</textarea>
            </div>`;
    },

    aposAbrir(pedido) {
        $("#btn-add-item").addEventListener("click", () => this.adicionarLinha());
        $("#itens-pedido").addEventListener("input", () => this.recalcular());
        $("#itens-pedido").addEventListener("change", () => this.recalcular());
        $("#itens-pedido").addEventListener("click", (e) => {
            if (e.target.matches(".item-remover")) { e.target.closest(".item-linha").remove(); this.recalcular(); }
        });
        (pedido?.itens?.length ? pedido.itens : [null]).forEach((i) => this.adicionarLinha(i));
    },

    adicionarLinha(item = null) {
        const opts = this.livros.map((l) => `<option value="${l.id}" data-preco="${l.preco}" ${item?.livroId === l.id ? "selected" : ""}>
            ${esc(l.titulo)} - ${moeda.format(l.preco)} (estoque: ${l.quantidadeEstoque})</option>`).join("");
        const div = document.createElement("div");
        div.className = "item-linha";
        div.innerHTML = `
            <select class="item-livro" aria-label="Livro"><option value="">Selecione o livro...</option>${opts}</select>
            <input class="item-qtd" type="number" min="1" value="${item?.quantidade ?? 1}" aria-label="Quantidade">
            <span class="subtotal">${moeda.format(0)}</span>
            <button class="btn btn-sm btn-danger item-remover" type="button" aria-label="Remover item">&times;</button>`;
        $("#itens-pedido").appendChild(div);
        this.recalcular();
    },

    recalcular() {
        let total = 0;
        document.querySelectorAll("#itens-pedido .item-linha").forEach((linha) => {
            const opt = $(".item-livro", linha).selectedOptions[0];
            const preco = Number(opt?.dataset.preco || 0);
            const sub = preco * Math.max(0, Number($(".item-qtd", linha).value || 0));
            $(".subtotal", linha).textContent = moeda.format(sub);
            total += sub;
        });
        $("#total-pedido").textContent = moeda.format(total);
    },

    ler() {
        const itens = [...document.querySelectorAll("#itens-pedido .item-linha")]
            .map((l) => ({ livroId: Number($(".item-livro", l).value), quantidade: Number($(".item-qtd", l).value) }))
            .filter((i) => i.livroId);
        let ok = true;
        if (!itens.length) { $('[data-campo="itens"]').textContent = "Inclua ao menos um item"; ok = false; }
        if (itens.some((i) => !(i.quantidade >= 1))) { $('[data-campo="itens"]').textContent = "Quantidade minima e 1"; ok = false; }
        const selCliente = $("#f-clienteId");
        if (selCliente && !selCliente.value) { $('[data-campo="clienteId"]').textContent = "Campo obrigatorio"; ok = false; }
        if (!ok) return null;
        return {
            clienteId: selCliente ? Number(selCliente.value) : null,
            itens,
            observacoes: $("#f-observacoes").value.trim() || null
        };
    }
};

async function abrirDetalhePedido(id) {
    let p;
    try { p = await api(`/pedidos/${id}`); } catch (err) { return mensagem(err.message); }
    const equipe = pode(EQUIPE);
    const itens = p.itens.map((i) => `<tr><td><div class="livro-com-capa">${capaClicavel({ id: i.livroId, capaUrl: i.capaUrl, titulo: i.livroTitulo }, "capa-micro")}${esc(i.livroTitulo)}</div></td><td>${i.quantidade}</td>
        <td>${dinheiro(i.valorUnitario)}</td><td>${dinheiro(i.subtotal)}</td></tr>`).join("");
    const pags = p.pagamentos.length
        ? p.pagamentos.map((g) => `<tr><td>${esc(rotuloForma[g.formaPagamento])}</td><td>${dinheiro(g.valor)}</td>
            <td>${statusBadge(g.status)}</td><td>${dataHora(g.dataPagamento)}</td></tr>`).join("")
        : `<tr><td colspan="4" class="vazio">Nenhum pagamento registrado.</td></tr>`;

    const rotuloStatus = { CANCELADO: "Cancelar pedido", ENVIADO: "Marcar como enviado", ENTREGUE: "Marcar como entregue" };
    const proximos = equipe ? p.proximosStatus : (p.status === "PENDENTE" ? ["CANCELADO"] : []);
    let acoes = proximos.map((s) => `<button class="btn btn-sm ${s === "CANCELADO" ? "btn-danger" : ""}" type="button"
        data-status-pedido="${s}">${rotuloStatus[s]}</button>`).join("");
    if (equipe && p.status === "PENDENTE" && Number(p.saldo) > 0) {
        acoes += `<button class="btn btn-sm btn-primary" type="button" id="btn-pagar">Registrar pagamento</button>`;
    }
    if (clientePodePagar(p)) {
        acoes += `<button class="btn btn-sm btn-primary" type="button" id="btn-pagar-cliente">Pagar pedido</button>`;
    }

    abrirModal({
        titulo: `Pedido #${p.id}`, largo: true, acoes,
        html: `<div class="inteira">
            <div class="detalhe">
                <div><small>Cliente</small>${esc(p.clienteNome)}</div>
                <div><small>Data</small>${dataHora(p.dataPedido)}</div>
                <div><small>Status</small>${statusBadge(p.status)}</div>
                <div><small>Total</small>${dinheiro(p.valorTotal)}</div>
                <div><small>Pago</small>${dinheiro(p.valorPago)}</div>
                <div><small>Saldo</small>${dinheiro(p.saldo)}</div>
            </div>
            ${p.observacoes ? `<p><small>Observacoes:</small> ${esc(p.observacoes)}</p>` : ""}
            <div class="subtitulo">Itens</div>
            <table class="tabela-mini" id="detalhe-itens"><thead><tr><th>Livro</th><th>Qtd</th><th>Unitario</th><th>Subtotal</th></tr></thead><tbody>${itens}</tbody></table>
            <div class="subtitulo">Pagamentos</div>
            <table class="tabela-mini" id="detalhe-pagamentos"><thead><tr><th>Forma</th><th>Valor</th><th>Status</th><th>Data</th></tr></thead><tbody>${pags}</tbody></table>
        </div>`
    });

    document.querySelectorAll("[data-status-pedido]").forEach((b) => b.addEventListener("click", async () => {
        const novo = b.dataset.statusPedido;
        if (novo === "CANCELADO" && !confirm("Confirma o cancelamento do pedido? O estoque sera devolvido.")) return;
        try {
            await api(`/pedidos/${p.id}/status`, { method: "PUT", body: { status: novo } });
            toast(`Pedido #${p.id} atualizado`);
            await carregar();
            await abrirDetalhePedido(p.id);
        } catch (err) { erroNoModal(err); }
    }));
    $("#btn-pagar")?.addEventListener("click", () => abrirPagamentoDoPedido(p));
    $("#btn-pagar-cliente")?.addEventListener("click", () => abrirPagamentoCliente(p));
}

function abrirPagamentoDoPedido(p) {
    camposAtuais = [];
    abrirModal({
        titulo: `Pagamento do pedido #${p.id}`,
        html: `
            <div class="campo"><label for="f-formaPagamento">Forma de pagamento *</label>
                <select id="f-formaPagamento">${Object.entries(rotuloForma).map(([v, t]) => `<option value="${v}">${t}</option>`).join("")}</select></div>
            <div class="campo"><label for="f-valor">Valor (R$) *</label>
                <input id="f-valor" type="number" step="0.01" value="${Number(p.saldo).toFixed(2)}">
                <div class="erro-campo" data-campo="valor"></div></div>
            <div class="campo inteira"><label class="checkbox"><input id="f-aprovar" type="checkbox" checked> Aprovar imediatamente</label></div>`,
        salvar: async () => {
            const pag = await api("/pagamentos", { method: "POST", body: {
                pedidoId: p.id, formaPagamento: $("#f-formaPagamento").value, valor: Number($("#f-valor").value)
            } });
            if ($("#f-aprovar").checked) {
                await api(`/pagamentos/${pag.id}/status`, { method: "PUT", body: { status: "APROVADO" } });
            }
            toast("Pagamento registrado");
            await carregar();
            await abrirDetalhePedido(p.id);
        }
    });
}

// ---------- Pagamento feito pelo proprio cliente ----------
const clientePodePagar = (p) => perfil() === CLIENTE && p.status === "PENDENTE" && Number(p.saldo) > 0;

const DICAS_PAGAMENTO = {
    PIX: "Aprovacao imediata.",
    CARTAO_CREDITO: "Cobranca no cartao de credito, aprovada na hora.",
    CARTAO_DEBITO: "Debito em conta, aprovado na hora.",
    BOLETO: "O pedido e marcado como pago assim que o boleto e registrado."
};

function abrirPagamentoCliente(p) {
    camposAtuais = [];
    formAtual = null;
    const opcoesForma = Object.entries(rotuloForma).map(([v, t]) => `
        <label class="forma-pagamento">
            <input type="radio" name="formaPagamento" value="${v}" ${v === "PIX" ? "checked" : ""}>
            <span><strong>${t}</strong><small>${DICAS_PAGAMENTO[v]}</small></span>
        </label>`).join("");
    const qtd = Number(p.quantidadeItens ?? p.itens?.length ?? 0);
    abrirModal({
        titulo: `Pagar pedido #${p.id}`,
        textoSalvar: "Confirmar pagamento",
        html: `<div class="inteira">
            <div class="resumo-pagamento">
                <span>${qtd} ${qtd === 1 ? "item" : "itens"}</span>
                <span>Total a pagar <strong>${moeda.format(Number(p.saldo))}</strong></span>
            </div>
            <div class="subtitulo">Forma de pagamento</div>
            <div class="formas-pagamento">${opcoesForma}</div>
        </div>`,
        salvar: async () => {
            const forma = $('#form-registro input[name="formaPagamento"]:checked')?.value;
            if (!forma) throw new Error("Escolha a forma de pagamento");
            const pago = await api(`/pedidos/${p.id}/pagamento`, { method: "POST", body: { formaPagamento: forma } });
            toast(`Pagamento do pedido #${pago.id} aprovado`);
            if (abaAtual === "pedidos") await carregar();
            await abrirDetalhePedido(pago.id);
        }
    });
}

// ---------- Livros de um autor, categoria ou editora ----------
const FILTRO_LIVROS = {
    autor: { titulo: "Livros do autor", teste: (l, id) => l.autores.some((a) => a.id === id) },
    categoria: { titulo: "Livros da categoria", teste: (l, id) => l.categoriaId === id },
    editora: { titulo: "Livros da editora", teste: (l, id) => l.editoraId === id }
};

function linkLivros(tipo, id, nome) {
    if (id == null) return esc(nome);
    return `<button class="link-livros" type="button" data-livros-de="${tipo}" data-livros-id="${id}"
        data-livros-nome="${esc(nome)}" title="Ver livros">${esc(nome)}</button>`;
}

async function abrirLivrosDe(tipo, id, nome) {
    const filtro = FILTRO_LIVROS[tipo];
    let livros;
    try { livros = await api("/livros"); } catch (err) { return mensagem(err.message); }
    livros = livros.filter((l) => filtro.teste(l, Number(id)) && (l.ativo || pode(EQUIPE)));
    const podeComprar = pode(TELAS.pedidos.criar);
    const outraColuna = tipo === "categoria" ? "Editora" : "Categoria";
    const linhas = livros.length
        ? livros.map((l) => `<tr>
            <td><div class="livro-com-capa">${capaClicavel(l, "capa-micro")}<div><strong>${esc(l.titulo)}</strong>
                <br><small>${esc(l.autores.map((a) => a.nome).join(", "))}</small></div></div></td>
            <td class="col-opcional">${esc(tipo === "categoria" ? l.editoraNome : l.categoriaNome)}</td>
            <td>${dinheiro(l.preco)}</td>
            <td>${!l.ativo ? badge("Inativo", "badge-off") : l.quantidadeEstoque > 0 ? esc(l.quantidadeEstoque) : badge("Esgotado", "badge-off")}</td>
            <td class="acoes">${podeComprar && l.ativo && l.quantidadeEstoque > 0
                ? `<button class="btn btn-sm btn-primary" type="button" data-comprar="${l.id}">Comprar</button>` : ""}</td>
        </tr>`).join("")
        : `<tr><td colspan="5" class="vazio">Nenhum livro encontrado.</td></tr>`;
    abrirModal({
        titulo: `${filtro.titulo}: ${nome}`, largo: true,
        html: `<div class="inteira">
            <p class="contagem">${livros.length} ${livros.length === 1 ? "livro" : "livros"}</p>
            <table class="tabela-mini"><thead><tr><th>Livro</th><th class="col-opcional">${outraColuna}</th>
                <th>Preco</th><th>Estoque</th><th></th></tr></thead><tbody>${linhas}</tbody></table>
        </div>`
    });
}

// ---------- Detalhes do livro (clique na capa) ----------
async function abrirDetalheLivro(id) {
    let l;
    try { l = await api(`/livros/${id}`); } catch (err) { return mensagem(err.message); }
    const disponivel = l.ativo && l.quantidadeEstoque > 0;
    const situacao = !l.ativo ? badge("Indisponivel", "badge-off")
        : l.quantidadeEstoque > 0 ? badge(`${l.quantidadeEstoque} em estoque`) : badge("Esgotado", "badge-off");
    const info = [
        ["Autor(es)", l.autores.length ? l.autores.map((a) => linkLivros("autor", a.id, a.nome)).join(", ") : "-"],
        ["Categoria", linkLivros("categoria", l.categoriaId, l.categoriaNome)],
        ["Editora", linkLivros("editora", l.editoraId, l.editoraNome)],
        ["ISBN", esc(l.isbn)],
        ["Ano", esc(l.anoPublicacao ?? "-")],
        ["Paginas", esc(l.numeroPaginas ?? "-")],
        ["Idioma", esc(l.idioma || "-")]
    ].map(([k, v]) => `<div><dt>${k}</dt><dd>${v}</dd></div>`).join("");
    const comprar = disponivel && pode(TELAS.pedidos.criar)
        ? `<button class="btn btn-primary" type="button" data-comprar="${l.id}">Comprar</button>` : "";
    abrirModal({
        titulo: "Detalhes do livro", largo: true,
        html: `<div class="inteira livro-detalhe">
            <div class="livro-detalhe-capa">${capaHtml(l, "capa-grande")}</div>
            <div class="livro-detalhe-info">
                <h4>${esc(l.titulo)}</h4>
                ${l.subtitulo ? `<p class="livro-subtitulo">${esc(l.subtitulo)}</p>` : ""}
                <div class="livro-preco">${dinheiro(l.preco)} ${situacao}</div>
                <dl class="livro-ficha">${info}</dl>
                ${l.descricao ? `<div class="subtitulo">Sinopse</div><p class="livro-descricao">${esc(l.descricao)}</p>` : ""}
            </div>
        </div>`,
        acoes: comprar
    });
}

async function comprarLivro(livroId) {
    if (abaAtual !== "pedidos") await trocarAba("pedidos");
    await abrirFormulario(null, { itens: [{ livroId, quantidade: 1 }] });
}

async function alterarStatusPagamento(id, status) {
    const verbo = { APROVADO: "aprovar", CANCELADO: "cancelar", ESTORNADO: "estornar" }[status];
    if (status !== "APROVADO" && !confirm(`Confirma ${verbo} este pagamento?`)) return;
    try {
        await api(`/pagamentos/${id}/status`, { method: "PUT", body: { status } });
        toast(`Pagamento ${status.toLowerCase()}`);
        await carregar();
    } catch (err) {
        mensagem(err.message);
    }
}

// ===========================================================================
// Auditoria
// ===========================================================================
function parseDetalhes(a) {
    try { return JSON.parse(a.detalhes || "{}"); } catch { return {}; }
}

function resumoAuditoria(a) {
    const d = parseDetalhes(a);
    const reg = d.depois || d.antes || {};
    const idCampo = Object.keys(reg).find((k) => k.startsWith("id_"));
    const nome = reg.titulo || reg.nome || reg.email || "";
    let texto = `${idCampo ? `${idCampo} = ${reg[idCampo]}` : ""}${nome ? ` (${nome})` : ""}`;
    if (a.operacao === "UPDATE" && d.antes && d.depois) {
        const mudou = Object.keys(d.depois).filter((k) => JSON.stringify(d.antes[k]) !== JSON.stringify(d.depois[k]));
        if (mudou.length) texto += ` - alterou: ${mudou.join(", ")}`;
    }
    return texto;
}

function abrirDetalheAuditoria(a) {
    const d = parseDetalhes(a);
    let corpo;
    if (a.operacao === "UPDATE" && d.antes && d.depois) {
        const linhas = Object.keys(d.depois).map((k) => {
            const mudou = JSON.stringify(d.antes[k]) !== JSON.stringify(d.depois[k]);
            return `<tr><td class="${mudou ? "diff-campo" : ""}">${esc(k)}</td><td>${esc(JSON.stringify(d.antes[k]))}</td><td>${esc(JSON.stringify(d.depois[k]))}</td></tr>`;
        }).join("");
        corpo = `<table class="tabela-mini"><thead><tr><th>Campo</th><th>Antes</th><th>Depois</th></tr></thead><tbody>${linhas}</tbody></table>`;
    } else {
        corpo = `<pre class="json">${esc(JSON.stringify(d.depois || d.antes, null, 2))}</pre>`;
    }
    abrirModal({
        titulo: `Auditoria #${a.id}`, largo: true,
        html: `<div class="inteira">
            <div class="detalhe">
                <div><small>Data/hora</small>${dataHora(a.dataEvento)}</div>
                <div><small>Tabela</small>${esc(a.tabela)}</div>
                <div><small>Operacao</small>${badge(a.operacao, `op-${a.operacao}`)}</div>
                <div><small>Usuario</small>${esc(a.usuario)}</div>
            </div>${corpo}</div>`
    });
}

// ===========================================================================
// Navegacao
// ===========================================================================
async function trocarAba(aba) {
    if (!TELAS[aba] || !pode(TELAS[aba].ver)) aba = "livros";
    abaAtual = aba;
    const tela = TELAS[aba];
    document.querySelectorAll(".tab").forEach((t) => t.classList.toggle("ativa", t.dataset.aba === aba));
    $("#titulo-aba").textContent = tela.titulo;
    $("#campo-busca").value = "";
    $("#btn-novo").classList.toggle("oculto", !podeCriarTela(tela));
    $("#btn-novo").textContent = tela.textoNovo || (aba === "pedidos" ? "Novo pedido" : aba === "pagamentos" ? "Registrar pagamento" : "Novo");
    history.replaceState(null, "", `#${aba}`);
    try {
        await prepararFiltro("#filtro", tela.filtro);
        await prepararFiltro("#filtro2", tela.filtro2);
    } catch (err) { mensagem(err.message); }
    await carregar();
}

function iniciar() {
    if (!sessao.token) {
        window.location.replace("index.html");
        return;
    }
    const u = sessao.usuario;
    $("#usuario-nome").textContent = u.nome;
    $("#usuario-perfil").textContent = u.perfil;

    $("#abas").innerHTML = Object.entries(TELAS)
        .filter(([, t]) => pode(t.ver))
        .map(([chave, t]) => `<button class="tab" type="button" data-aba="${chave}">${t.titulo}</button>`).join("");

    $("#abas").addEventListener("click", (e) => { if (e.target.dataset.aba) trocarAba(e.target.dataset.aba); });
    $("#form-busca").addEventListener("submit", (e) => { e.preventDefault(); carregar(); });
    $("#filtro").addEventListener("change", carregar);
    $("#filtro2").addEventListener("change", carregar);
    $("#btn-novo").addEventListener("click", () => abrirFormulario(null));
    $("#tabela-corpo").addEventListener("click", (e) => {
        const tela = TELAS[abaAtual];
        const link = e.target.closest("[data-livros-de]");
        if (link) return abrirLivrosDe(link.dataset.livrosDe, link.dataset.livrosId, link.dataset.livrosNome);
        const b = e.target.closest("button[data-acao]");
        if (!b) {
            const tr = e.target.closest("tr.linha-clicavel");
            if (tr && !e.target.closest("a")) tela.aoClicarLinha(itensAtuais.find((i) => String(i.id) === tr.dataset.id));
            return;
        }
        const item = itensAtuais.find((i) => String(i.id) === b.dataset.id);
        if (b.dataset.acao === "editar") abrirFormulario(b.dataset.id);
        else if (b.dataset.acao === "excluir") excluir(b.dataset.id);
        else tela.aoAcao?.(b.dataset.acao, b.dataset.id, item);
    });
    document.addEventListener("click", (e) => {
        const capa = e.target.closest("[data-detalhe-livro]");
        if (capa) return abrirDetalheLivro(capa.dataset.detalheLivro);
        const comprar = e.target.closest("#modal [data-comprar]");
        if (comprar) return comprarLivro(Number(comprar.dataset.comprar));
        const link = e.target.closest("#modal [data-livros-de]");
        if (link) abrirLivrosDe(link.dataset.livrosDe, link.dataset.livrosId, link.dataset.livrosNome);
    });
    $("#tabela-corpo").addEventListener("keydown", (e) => {
        const tr = e.target.closest?.("tr.linha-clicavel");
        if (tr && e.target === tr && (e.key === "Enter" || e.key === " ")) { e.preventDefault(); tr.click(); }
    });
    $("#form-registro").addEventListener("submit", aoSubmeter);
    $("#btn-fechar").addEventListener("click", () => $("#modal").close());
    $("#btn-cancelar").addEventListener("click", () => $("#modal").close());
    $("#btn-sair").addEventListener("click", () => { sessao.limpar(); window.location.href = "index.html"; });

    trocarAba(location.hash.slice(1) || "livros");
}

iniciar();
