// Abertura animada (antes da tela de login): a semente cai no livro aberto, brota e vira uma arvore adulta,
// enquanto folhas passam sobre o livro ao fundo. Exibida uma vez por sessao do navegador; pode ser pulada.
(function () {
    const raiz = document.documentElement;
    if (!raiz.classList.contains("com-intro")) return;

    const DURACAO = 7200; // ms ate a abertura comecar a sumir
    // folha com ponta e base assimetricas; nervura central e laterais desenhadas por cima
    const FOLHA = "M0 0 C 5 -8 15 -11 24 -6 C 27 -4 29 -1.5 31 0 C 27 3 21 9 12 8 C 6 7 2 4 0 0 Z";
    const NERVURA = "M0.5 0 C 10 -0.8 21 -1 30 0 M9 -0.5 L 14 -5.5 M16 -0.6 L 21 -5 M9 -0.5 L 13 4.5 M16 -0.6 L 20 4";
    // degrades das folhas (base escura -> ponta clara): verdes de sombra, de meia-luz, iluminados e algumas douradas
    const TONS = {
        sombra: [["#0f2410", "#21431f", "#3b6431"], ["#14301a", "#2b5230", "#4a7646"]],
        meia: [["#1d3d18", "#3c6a2b", "#6c9a47"], ["#26471b", "#4b7d33", "#86ad55"]],
        luz: [["#3a5a1a", "#6f9a36", "#b3cc6a"], ["#45601d", "#86a83c", "#c9d97c"]],
        ouro: [["#6e4612", "#b07d2a", "#e2bf66"]]
    };
    const gradientes = [];
    const idTom = {};
    Object.entries(TONS).forEach(([grupo, lista]) => lista.forEach((c, i) => {
        const id = `intro-f-${grupo}-${i}`;
        (idTom[grupo] = idTom[grupo] || []).push(id);
        gradientes.push(`<linearGradient id="${id}" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0" stop-color="${c[0]}"/><stop offset=".55" stop-color="${c[1]}"/><stop offset="1" stop-color="${c[2]}"/></linearGradient>`);
    }));
    const folhaSvg = (id, classe = "", estilo = "") =>
        `<g class="${classe}" style="${estilo}"><path d="${FOLHA}" fill="url(#${id})"/>
            <path d="${NERVURA}" fill="none" stroke="#f3f0d0" stroke-opacity=".32" stroke-width=".7" stroke-linecap="round"/></g>`;

    // gerador deterministico: a copa sai igual em toda abertura
    let semente = 7;
    const aleatorio = () => ((semente = (semente * 16807) % 2147483647) / 2147483647);
    const sortear = (lista) => lista[Math.floor(aleatorio() * lista.length)];

    // ---------- copa: folhas numa elipse acima do tronco; a luz vem do alto, a esquerda
    const folhas = [];
    const N = 150, cx = 400, cy = 205, rx = 180, ry = 114;
    for (let i = 0; i < N; i++) {
        const r = Math.sqrt((i + 0.5) / N);
        const a = i * 2.39996;
        const x = cx + Math.cos(a) * r * rx + (aleatorio() - .5) * 14;
        const y = cy + Math.sin(a) * r * ry + (aleatorio() - .5) * 14;
        const ang = Math.round(aleatorio() * 360);
        const esc = (0.8 + aleatorio() * 0.5).toFixed(2);
        // -1 (sombra, embaixo a direita) .. +1 (luz, em cima a esquerda); o miolo da copa fica mais escuro
        const luz = ((cx - x) / rx) * 0.45 + ((cy - y) / ry) * 0.55 - (1 - r) * 0.35 + (aleatorio() - .5) * 0.5;
        const grupo = aleatorio() < 0.09 ? "ouro" : luz > 0.3 ? "luz" : luz < -0.25 ? "sombra" : "meia";
        const atraso = (3.25 + r * 1.35 + aleatorio() * 0.35).toFixed(2);
        folhas.push({ y, html: `<g transform="translate(${x.toFixed(1)} ${y.toFixed(1)}) rotate(${ang}) scale(${esc})">
            ${folhaSvg(sortear(idTom[grupo]), "intro-folha", `animation-delay:${atraso}s`)}</g>` });
    }
    // folhas de tras primeiro (as de baixo, na sombra, ficam por baixo das iluminadas)
    folhas.sort((f1, f2) => f2.y - f1.y);

    // ---------- galhos (desenhados com stroke-dashoffset); cada galho tem lado de sombra e de luz
    const galhos = [
        ["M400 322 C 362 304 330 282 302 244", 7, 2.55],
        ["M400 300 C 440 286 470 262 498 222", 7, 2.65],
        ["M400 262 C 372 242 352 212 336 172", 6, 2.85],
        ["M400 246 C 430 226 450 196 462 160", 6, 2.95],
        ["M400 214 C 386 192 380 162 382 128", 5, 3.1],
        ["M400 204 C 414 182 424 152 424 124", 5, 3.15],
        ["M330 276 C 310 270 290 270 268 276", 3.5, 3.05],
        ["M472 250 C 494 246 514 250 534 262", 3.5, 3.1],
        ["M352 214 C 338 204 322 202 306 206", 2.5, 3.2],
        ["M452 196 C 468 186 484 184 500 188", 2.5, 3.25],
    ].map(([d, w, t]) => `
        <path class="intro-traco" d="${d}" pathLength="1" stroke="url(#intro-galho)" stroke-width="${w}" style="animation-delay:${t}s"/>
        <path class="intro-traco" d="${d}" pathLength="1" stroke="#f0d9a8" stroke-opacity=".28" stroke-width="${(w / 3).toFixed(1)}"
              transform="translate(-1 -1.2)" style="animation-delay:${t}s"/>`).join("");

    // ---------- livro aberto; a pagina da direita vira em loop
    const capaEsq = "M400 508 Q 250 472 86 498 L 126 374 Q 268 357 400 420 Z";
    // pagina com a superficie curva: sobe a partir da lombada e se abre para fora (como um livro de verdade)
    const paginaEsq = "M400 494 C 330 468 210 462 110 480 L 146 372 C 230 356 330 360 382 392 C 392 398 397 404 400 410 Z";
    const espelho = 'transform="translate(800 0) scale(-1 1)"';
    const espelharX = (d) => d.replace(/(-?\d+(?:\.\d+)?) (-?\d+(?:\.\d+)?)/g, (_, x, y) => `${800 - Number(x)} ${y}`);

    // bloco de folhas visto pelas bordas: embaixo e na lateral de fora, entre a pagina e a capa
    const bordas = [1, 2, 3, 4, 5, 6].map((i) => `
        <path class="intro-borda" d="M400 ${494 + i * 2.2} C 330 ${468 + i * 2.2} 210 ${462 + i * 2.3} ${110 - i * 1.6} ${480 + i * 2.5}"/>
        <path class="intro-borda" d="M${110 - i * 1.6} ${480 + i * 2.5} L ${146 - i * 2.4} ${372 + i * 0.6}"/>`).join("");

    // texto impresso: linhas que acompanham a curvatura da pagina, com espacos entre palavras e paragrafos
    const lerp = (a, b, t) => a + (b - a) * t;
    function textoPagina(espelhar) {
        const linhas = [];
        const n = 15;
        for (let k = 0; k < n; k++) {
            const t = 0.1 + (k / (n - 1)) * 0.76;
            let x0 = lerp(146, 110, t) + 22, y0 = lerp(374, 478, t);
            let x1 = lerp(386, 397, t) - 24, y1 = lerp(396, 486, t) - 3;
            let cx = lerp(268, 255, t), cy = lerp(357, 464, t);
            if (k === 0 || k === 5 || k === 11) { // recuo da primeira linha do paragrafo
                const u = 0.06;
                const nx = lerp(lerp(x0, cx, u), lerp(cx, x1, u), u), ny = lerp(lerp(y0, cy, u), lerp(cy, y1, u), u);
                cx = lerp(cx, x1, u); cy = lerp(cy, y1, u); x0 = nx; y0 = ny;
            }
            if (k === 4 || k === 10 || k === n - 1) { // ultima linha do paragrafo e mais curta
                const u = 0.35 + aleatorio() * 0.3;
                const nx = lerp(lerp(x0, cx, u), lerp(cx, x1, u), u), ny = lerp(lerp(y0, cy, u), lerp(cy, y1, u), u);
                cx = lerp(x0, cx, u); cy = lerp(y0, cy, u); x1 = nx; y1 = ny;
            }
            // tracos = palavras; o padrao muda a cada linha para nao parecer repetido
            const palavras = Array.from({ length: 10 }, () =>
                `${(3 + aleatorio() * 9).toFixed(1)} ${(1.6 + aleatorio() * 0.8).toFixed(1)}`).join(" ");
            const f = (v) => v.toFixed(1);
            let d = `M${f(x0)} ${f(y0)} Q ${f(cx)} ${f(cy)} ${f(x1)} ${f(y1)}`;
            if (espelhar) d = espelharX(d);
            linhas.push(`<path d="${d}" stroke-dasharray="${palavras}"/>`);
        }
        return linhas.join("");
    }
    const linhasEsq = textoPagina(false);
    const linhasDir = textoPagina(true);

    // pagina virando: o caminho muda de forma (deitada a direita -> levantando -> em pe -> descendo -> deitada a esquerda)
    const virar = {
        a: espelharX(paginaEsq),
        a1: "M400 494 C 470 470 570 440 630 400 L 590 290 C 528 296 462 338 420 384 C 410 396 404 404 400 410 Z",
        b: "M400 494 C 446 462 474 392 470 322 L 436 222 C 432 286 414 350 404 386 C 402 396 401 404 400 410 Z",
        c1: "M400 494 C 330 470 230 440 170 400 L 210 290 C 272 296 338 338 380 384 C 390 396 396 404 400 410 Z",
        c: paginaEsq
    };
    const formas = [virar.a, virar.a1, virar.b, virar.c1, virar.c, virar.c].join(";");
    const tempos = "0;.28;.5;.72;.9;1";
    const curvas = ".45 0 .55 1;.3 0 .7 1;.3 0 .7 1;.45 0 .55 1;0 0 1 1";
    const paginaVirando = (inicio) => `
      <g opacity="0">
        <animate attributeName="opacity" values="0;1;1;0;0" keyTimes="0;.06;.64;.84;1" dur="2.8s" begin="${inicio}s" repeatCount="indefinite"/>
        <path d="${virar.a}" fill="#2a1d0c" opacity="0" filter="url(#intro-desfoque-leve)">
          <animate attributeName="d" values="${formas}" keyTimes="${tempos}" calcMode="spline" keySplines="${curvas}" dur="2.8s" begin="${inicio}s" repeatCount="indefinite"/>
          <animate attributeName="opacity" values="0;.22;.12;.05;0;0" keyTimes="${tempos}" dur="2.8s" begin="${inicio}s" repeatCount="indefinite"/>
        </path>
        <path d="${virar.a}" fill="url(#intro-papel-virando)">
          <animate attributeName="d" values="${formas}" keyTimes="${tempos}" calcMode="spline" keySplines="${curvas}" dur="2.8s" begin="${inicio}s" repeatCount="indefinite"/>
        </path>
        <path d="${virar.a}" fill="#5c4a2a" opacity="0">
          <animate attributeName="d" values="${formas}" keyTimes="${tempos}" calcMode="spline" keySplines="${curvas}" dur="2.8s" begin="${inicio}s" repeatCount="indefinite"/>
          <animate attributeName="opacity" values="0;.06;.22;.08;0;0" keyTimes="${tempos}" dur="2.8s" begin="${inicio}s" repeatCount="indefinite"/>
        </path>
      </g>`;

    const svg = `
<svg class="intro-cena" viewBox="0 0 800 600" role="img" aria-label="Uma semente cai em um livro aberto e cresce ate virar uma arvore">
  <defs>
    ${gradientes.join("")}
    <linearGradient id="intro-casca" x1="0" y1="0" x2="1" y2="0">
      <stop offset="0" stop-color="#3b2412"/><stop offset=".3" stop-color="#6b4424"/><stop offset=".55" stop-color="#8e6236"/>
      <stop offset=".8" stop-color="#5c3a1d"/><stop offset="1" stop-color="#2f1c0d"/>
    </linearGradient>
    <linearGradient id="intro-galho" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#7a5230"/><stop offset="1" stop-color="#4a2f17"/>
    </linearGradient>
    <linearGradient id="intro-capa" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#24503d"/><stop offset=".45" stop-color="#173a2c"/><stop offset="1" stop-color="#0b2219"/>
    </linearGradient>
    <linearGradient id="intro-papel" x1="1" y1="0" x2="0" y2="0">
      <stop offset="0" stop-color="#bfae88"/><stop offset=".06" stop-color="#ddd0b2"/><stop offset=".22" stop-color="#f1e8d4"/>
      <stop offset=".6" stop-color="#faf5ea"/><stop offset=".9" stop-color="#f6efe0"/><stop offset="1" stop-color="#ece2cc"/>
    </linearGradient>
    <linearGradient id="intro-papel-luz" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#ffffff" stop-opacity=".55"/><stop offset=".5" stop-color="#ffffff" stop-opacity="0"/>
      <stop offset="1" stop-color="#8a7650" stop-opacity=".12"/>
    </linearGradient>
    <linearGradient id="intro-papel-virando" x1="1" y1="0" x2="0" y2="0">
      <stop offset="0" stop-color="#d8caa8"/><stop offset=".35" stop-color="#f2e9d6"/><stop offset="1" stop-color="#fffbf3"/>
    </linearGradient>
    <radialGradient id="intro-semente-g" cx=".35" cy=".3" r=".8">
      <stop offset="0" stop-color="#c99556"/><stop offset=".45" stop-color="#8a5a26"/><stop offset="1" stop-color="#4a2e10"/>
    </radialGradient>
    <radialGradient id="intro-brilho"><stop offset="0" stop-color="#fff6d8"/><stop offset="1" stop-color="#fff6d8" stop-opacity="0"/></radialGradient>
    <filter id="intro-desfoque" x="-20%" y="-50%" width="140%" height="200%"><feGaussianBlur stdDeviation="6"/></filter>
    <filter id="intro-desfoque-forte" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="16"/></filter>
    <filter id="intro-desfoque-leve" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="2.5"/></filter>
  </defs>

  <g class="intro-livro">
    <ellipse cx="400" cy="516" rx="320" ry="20" fill="#3a2a14" opacity=".28" filter="url(#intro-desfoque)"/>
    <path d="${capaEsq}" fill="url(#intro-capa)"/><path d="${capaEsq}" fill="url(#intro-capa)" ${espelho}/>
    <path d="M400 508 Q 250 472 86 498" fill="none" stroke="#3f6d55" stroke-opacity=".5" stroke-width="1.5"/>
    <path d="M400 508 Q 250 472 86 498" fill="none" stroke="#3f6d55" stroke-opacity=".5" stroke-width="1.5" ${espelho}/>
    <g>${bordas}</g><g ${espelho}>${bordas}</g>
    <path d="${paginaEsq}" fill="url(#intro-papel)"/><path d="${paginaEsq}" fill="url(#intro-papel)" ${espelho}/>
    <path d="${paginaEsq}" fill="url(#intro-papel-luz)"/><path d="${paginaEsq}" fill="url(#intro-papel-luz)" ${espelho}/>
    <g class="intro-linhas">${linhasEsq}</g>
    <g class="intro-linhas">${linhasDir}</g>
    <!-- vinco da lombada: sombra funda no meio e brilho nas bordas das paginas -->
    <path d="M400 410 C 397 440 397 470 400 494" fill="none" stroke="#5b4626" stroke-opacity=".5" stroke-width="16" filter="url(#intro-desfoque-leve)"/>
    <path d="M400 410 C 398 440 398 470 400 494" fill="none" stroke="#3d2e17" stroke-opacity=".55" stroke-width="2"/>
    ${paginaVirando(0.9)}
    ${paginaVirando(2.3)}
  </g>

  <circle class="intro-brilho" cx="400" cy="408" r="46" fill="url(#intro-brilho)"/>

  <g class="intro-arvore">
    <ellipse class="intro-sombra-copa" cx="410" cy="226" rx="140" ry="82" fill="#1d3a1a" filter="url(#intro-desfoque-forte)"/>
    <g class="intro-raizes">
      <path d="M392 414 C 384 418 374 420 362 419 C 376 414 386 408 392 402 Z" fill="#5a3a1d"/>
      <path d="M408 414 C 416 418 428 421 440 419 C 426 414 416 408 408 402 Z" fill="#4a2f17"/>
    </g>
    <path class="intro-traco intro-tronco" d="M400 414 C 394 372 408 338 400 290" pathLength="1" stroke="url(#intro-casca)" stroke-width="17" style="animation-delay:1.85s"/>
    <path class="intro-traco intro-tronco" d="M400 296 C 394 258 404 226 400 176" pathLength="1" stroke="url(#intro-casca)" stroke-width="11" style="animation-delay:2.35s"/>
    <!-- textura da casca: fissuras escuras e um reflexo do lado iluminado -->
    <path class="intro-traco intro-tronco" d="M397 410 C 392 372 405 340 398 296" pathLength="1" stroke="#2a180a" stroke-opacity=".45" stroke-width="1.6" style="animation-delay:1.95s"/>
    <path class="intro-traco intro-tronco" d="M404 404 C 400 370 410 344 403 300" pathLength="1" stroke="#2a180a" stroke-opacity=".35" stroke-width="1.2" style="animation-delay:2s"/>
    <path class="intro-traco intro-tronco" d="M395 400 C 390 366 402 336 395 292 C 390 258 399 228 397 182" pathLength="1" stroke="#e8c995" stroke-opacity=".35" stroke-width="2" style="animation-delay:2.1s"/>
    ${galhos}
    <g class="intro-broto">
      <path d="M400 412 C 398 398 402 388 400 376" stroke="#6f8f3a" stroke-width="2.6" fill="none" stroke-linecap="round"/>
      <g transform="translate(400 380) rotate(-150) scale(.55)">${folhaSvg(idTom.luz[0])}</g>
      <g transform="translate(400 378) rotate(-30) scale(.55)">${folhaSvg(idTom.meia[1])}</g>
    </g>
    <g class="intro-copa">${folhas.map((f) => f.html).join("")}</g>
  </g>

  <ellipse class="intro-semente" cx="400" cy="402" rx="6.5" ry="9.5" fill="url(#intro-semente-g)"/>

  <text class="intro-nome" x="400" y="556" text-anchor="middle">Páginas Vivas</text>
  <text class="intro-slogan" x="400" y="586" text-anchor="middle">Mais que livros, novas histórias para você.</text>
</svg>`;

    // folhas que atravessam a cena por cima do livro (usam os mesmos degrades da copa)
    const tonsVoo = [...idTom.meia, ...idTom.luz, ...idTom.ouro, ...idTom.ouro];
    const voando = Array.from({ length: 9 }, (_, i) => {
        const topo = 8 + ((i * 37) % 70);
        const dur = (5.2 + (i % 4) * 0.9).toFixed(1);
        const atraso = (1.1 + i * 0.55).toFixed(2);
        const tam = 22 + (i % 3) * 8;
        return `<div class="intro-voo" style="top:${topo}%;animation-duration:${dur}s;animation-delay:${atraso}s">
            <svg viewBox="-2 -12 35 24" width="${tam}" height="${tam}" style="animation-duration:${(dur / 3).toFixed(1)}s">
              ${folhaSvg(tonsVoo[i % tonsVoo.length])}</svg></div>`;
    }).join("");

    const camada = document.createElement("div");
    camada.className = "intro";
    camada.innerHTML = `${voando}<div class="intro-palco">${svg}</div>
        <button type="button" class="intro-pular">Pular animação</button>`;
    document.body.appendChild(camada);

    let terminou = false;
    function terminar() {
        if (terminou) return;
        terminou = true;
        camada.classList.add("intro-saindo");
        raiz.classList.remove("com-intro");
        setTimeout(() => camada.remove(), 900);
        document.removeEventListener("keydown", tecla);
    }
    const tecla = (e) => { if (e.key === "Escape" || e.key === "Enter" || e.key === " ") terminar(); };
    camada.querySelector(".intro-pular").addEventListener("click", terminar);
    camada.addEventListener("click", terminar); // um clique em qualquer lugar pula a abertura
    document.addEventListener("keydown", tecla);
    setTimeout(terminar, DURACAO);
})();
