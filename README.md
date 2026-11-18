# Páginas Vivas Livraria — Sistema de Vendas de Livros

*Mais que livros, novas histórias para você.*

Sistema Full Stack (cliente-servidor) com back-end em **Spring Boot 4 / Java 27**, banco **PostgreSQL**
e front-end em **HTML + CSS + JavaScript** que consome a API REST.
Há também um **app Android nativo em Java** (catálogo, carrinho, pedidos e pagamento) na pasta [`android/`](android/README.md).

Integrantes: Leonidio Junior | Vitor Inácio Prudente

## Atendimento aos requisitos da atividade

| Requisito | Como foi atendido |
|---|---|
| Tema: Sistema de Vendas de Livros | Catálogo (livros, autores, editoras, categorias), estoque, clientes, pedidos e pagamentos |
| Banco relacional PostgreSQL | Spring Data JPA + PostgreSQL 17; esquema versionado com Flyway (`V1` a `V7`), com restrições, índices e triggers |
| Entidade para controle de usuários no login | `Usuario` (nome, e-mail, senha em hash BCrypt, perfil, ativo, data de cadastro, último acesso) |
| Cada entidade com no mínimo 5 atributos | Todas as entidades têm 5 ou mais atributos (tabela abaixo) |
| 2 CRUDs de entidades de negócio (além de Usuário) | 7 CRUDs completos: **Livros, Autores, Editoras, Categorias, Clientes, Estoque e Pedidos** |
| CRUD de Usuário com tela de login funcional | Aba *Usuarios* (ADMIN) + `/api/usuarios`; tela de login com autenticação JWT e auto-cadastro de cliente |
| Arquitetura cliente-servidor | Back-end: API REST com as regras de negócio. Front-end: páginas que consomem a API via `fetch` |
| Testes automatizados em diferentes níveis | **154 testes**: 71 unitários, 63 de integração, 19 ponta a ponta (E2E com navegador) e 1 de carga do contexto |

**Entidades e quantidade de atributos** (campos da entidade, incluindo a chave e as chaves estrangeiras, como as colunas da tabela):

| Entidade | Atributos | Entidade | Atributos |
|---|:-:|---|:-:|
| Livro | 16 | Usuario | 8 |
| Cliente | 14 | Pagamento | 7 |
| Pedido | 8 | ItemPedido | 6 |
| Editora | 6 | Auditoria | 6 |
| Autor | 6 | LivroCapa / AutorFoto (imagens) | 6 |
| Categoria | 5 | Estoque | 5 |
| Perfil | 5 | | |

| Módulo | API | Tela (aba) |
|---|---|---|
| Controle de usuários + login (JWT) | `/api/auth`, `/api/usuarios` | Login, *Usuarios* |
| Perfis de acesso (ADMIN, VENDEDOR, CLIENTE) | `/api/perfis` | — |
| Autores + **foto** | `/api/autores`, `/api/autores/{id}/foto` | *Autores* |
| Editoras | `/api/editoras` | *Editoras* |
| Categorias | `/api/categorias` | *Categorias* |
| Livros (50 de exemplo) + **capa** | `/api/livros`, `/api/livros/{id}/capa` | *Livros* |
| Estoque | `/api/estoque` | *Estoque* |
| Clientes | `/api/clientes` | *Clientes* |
| Pedidos e itens do pedido | `/api/pedidos` | *Pedidos* (com tela de detalhes) |
| Pagamentos | `/api/pagamentos` | *Pagamentos* |
| Auditoria básica (triggers) | `/api/auditoria` | *Auditoria* |
| Índices e restrições | migração `V4` | — |
| Testes automatizados | — | 154 testes (unitários, integração, E2E) |

## Arquitetura

```
Navegador (static/index.html, app.html, js/*.js)
     │  fetch + "Authorization: Bearer <JWT>"
     ▼
Controllers (REST) ──► Services (regras de negócio) ──► Repositories (Spring Data JPA) ──► PostgreSQL
     ▲                                                                                     │
SecurityConfig + JwtAuthenticationFilter                       triggers fn_auditoria ──► tabela auditoria
```

Pacotes em `com.example.livraria` (classe principal `LivrariaApplication`): `domain` (entidades JPA), `repository`, `service`, `controller`,
`dto`, `security`, `config`, `exception`. O esquema do banco é versionado com Flyway em
`src/main/resources/db/migration`:

| Migração | Conteúdo |
|---|---|
| V1 | Tabelas (esquema `livraria`) |
| V2 | Perfis, categorias e editoras iniciais |
| V3 | 50 livros de exemplo, 56 autores e 24 editoras (ISBNs fictícios `999-00-0000-NNN`) |
| V4 | Perfil VENDEDOR, nome/e-mail do cliente, estoque inicial, restrições, índices e triggers de auditoria |
| V5 | Tabelas `livro_capa` e `autor_foto` (imagens) |
| V6 | Índices únicos de CPF e e-mail do cliente (impede cadastro em duplicidade) |
| V7 | Atributos das tabelas de apoio: situação e data de cadastro do perfil; nome do arquivo, tamanho e data de envio das imagens |

## Perfis de acesso

| Ação | ADMIN | VENDEDOR | CLIENTE |
|---|:-:|:-:|:-:|
| Consultar livros, autores, editoras e categorias | ✔ | ✔ | ✔ |
| Cadastrar/alterar/excluir livros, autores, editoras e categorias | ✔ | ✖ | ✖ |
| Estoque, clientes e pagamentos | ✔ | ✔ | ✖ |
| Criar pedido | ✔ (para qualquer cliente) | ✔ (para qualquer cliente) | ✔ (só para si) |
| Ver pedidos | todos | todos | só os próprios |
| Pagar pedido | registra/aprova pagamentos | registra/aprova pagamentos | paga o próprio pedido pendente |
| Alterar itens / excluir pedido | ✔ | ✔ | ✖ |
| Mudar status do pedido | ✔ | ✔ | só cancelar o próprio pendente |
| Usuários e auditoria | ✔ | ✖ | ✖ |

Bloqueios de acesso retornam **403**; requisições sem login retornam **401**. A interface mostra apenas
as abas e botões permitidos para o perfil.

## Regras de negócio

**Vendas**
- O preço de cada item é o preço atual do livro e o total é calculado no servidor. Itens repetidos do mesmo livro são somados.
- Criar o pedido **baixa o estoque**, com trava de linha para não vender a mesma unidade duas vezes. Estoque insuficiente retorna 409.
- Alterar os itens (só pedido PENDENTE) devolve o estoque antigo e baixa o novo.
- Cancelar o pedido devolve o estoque, cancela pagamentos pendentes e estorna os aprovados.
- Status do pedido: `PENDENTE → PAGO → ENVIADO → ENTREGUE`; cancelamento só a partir de PENDENTE ou PAGO.
  O pedido passa a **PAGO automaticamente** quando os pagamentos aprovados cobrem o total.
- Pagamento só pode ser registrado para pedido PENDENTE e sem ultrapassar o saldo em aberto.
  Status do pagamento: `PENDENTE → APROVADO | CANCELADO` e `APROVADO → ESTORNADO`. Estornar devolve um pedido PAGO para PENDENTE; não é permitido estornar depois do envio.
- Pedido com pagamentos não pode ser excluído (deve ser cancelado).
- **Pagamento pelo cliente**: o CLIENTE paga o próprio pedido pendente escolhendo PIX, cartão de crédito, cartão de débito ou boleto.
  O valor é o saldo em aberto e o pagamento é aprovado na hora (simulado; nenhum dado de cartão é pedido ou guardado).
  Logo depois de criar um pedido, o cliente é levado à tela de pagamento.

**Estoque**
- Todo livro já nasce com registro de estoque (zerado). A **Entrada de estoque** soma a quantidade recebida ao saldo atual.
- A **edição** do estoque altera somente o estoque mínimo. A quantidade só muda por entradas, vendas e cancelamentos.

**Cadastros**
- E-mail de usuário, ISBN, CPF, nome de categoria/editora e CNPJ são únicos (409).
- **Cliente sem duplicidade**: e-mail e CPF são obrigatórios no cadastro de cliente (aba *Clientes*) e no auto-cadastro da tela de login.
  Se já existir cliente com o mesmo CPF ou e-mail, o sistema avisa *"Cliente ja possui cadastro ativo..."* (409). A regra vale nos dois
  sentidos e também é garantida no banco (índices únicos da `V6`).
- O CPF tem os dígitos verificadores validados; UF, CEP e CNPJ têm o formato validado.
- Categoria/editora com livros, ou cliente com pedidos, não podem ser excluídos.
- Todo livro novo nasce com estoque zerado, que é removido junto com o livro.
- Um usuário com perfil CLIENTE (auto-cadastro ou criado pelo admin) ganha um cadastro de cliente vinculado, para poder comprar.
  Se o admin criar o login com o e-mail de um cliente já cadastrado pela loja, o login é vinculado a esse cadastro (sem duplicar).
  Ao excluir o usuário, o cliente e seus pedidos permanecem.
- O admin não pode excluir nem rebaixar a si mesmo, e sempre existe ao menos um ADMIN ativo.

## Capa do livro e foto do autor

- No formulário de livro/autor (ADMIN), o campo de imagem mostra uma pré-visualização antes de salvar. Também dá para remover a imagem atual.
- Formatos aceitos: **JPG, PNG ou WEBP, até 2 MB**. O tipo é conferido pelos primeiros bytes do arquivo, e não pelo nome ou extensão; arquivo que não é imagem retorna 400.
- As imagens ficam no PostgreSQL (`bytea`) em tabelas próprias. Assim, as listagens não carregam os bytes, e a imagem é apagada junto com o livro/autor.
- A leitura (`GET /api/livros/{id}/capa`, `GET /api/autores/{id}/foto`) é pública para funcionar em `<img>`. Enviar (`PUT`, multipart, campo `arquivo`) e remover (`DELETE`) exigem ADMIN.
- A resposta do livro/autor traz `capaUrl`/`fotoUrl` com a versão (`?v=`), o que permite cache longo no navegador. Sem imagem, a tela mostra as iniciais do título/nome.
- Junto com a imagem são gravados o nome do arquivo enviado, o tamanho em bytes e a data do envio.
- Os 50 livros de exemplo vêm **sem capa**: as capas reais são material protegido por direitos autorais, então devem ser enviadas por quem tiver o direito de usá-las.

## Navegação no catálogo

- **Detalhes do livro**: clicar na capa (na aba *Livros*, nas listas e nos itens do pedido) abre título, preço, estoque, autores,
  categoria, editora, ISBN, ano, páginas, idioma e sinopse, com o botão **Comprar**.
- **Livros por autor, categoria ou editora**: clicar numa linha das abas *Autores*, *Categorias* ou *Editoras*, ou no nome do
  autor/categoria/editora na aba *Livros*, mostra a lista de livros correspondente.
- **Comprar**: abre um novo pedido já com o livro escolhido; para o cliente, o pagamento vem em seguida.

## Identidade visual

- **Marca Páginas Vivas**: logo no cabeçalho do painel e no topo da tela de login, com o slogan *"Mais que livros, novas histórias para você."*
- **Abertura animada** (antes do login, uma vez por sessão do navegador): uma semente cai num livro aberto, brota e cresce até virar
  uma árvore adulta, enquanto as páginas viram e folhas passam sobre o livro. Feita só com SVG e CSS (`static/js/intro.js`), com tons
  realistas (luz e sombra nas folhas, casca no tronco, papel com textura).
  - Pode ser pulada (botão *Pular animação*, clique ou Esc/Enter).
  - Não aparece para quem configurou o sistema para reduzir animações, nem em testes automatizados (`navigator.webdriver`).
- **Tela de login** em verde-escuro e dourado (cores da marca), com ramos e pilhas de livros nas laterais.
- **Painel**: em telas largas (a partir de 1360 px), as faixas laterais livres ganham a mesma ilustração (`static/img/deco-lateral*.svg`).
- O painel mantém a paleta preto/branco com detalhes em vermelho e azul (variáveis CSS em `static/css/style.css`).

## Auditoria

Triggers `AFTER INSERT/UPDATE/DELETE` em usuario, autor, editora, categoria, livro, estoque, cliente,
pedido, item_pedido e pagamento gravam na tabela `auditoria`:
- a tabela e a operação;
- **o e-mail do usuário logado**: a aplicação define a variável de sessão `app.usuario` em cada conexão;
- data/hora;
- o registro **antes/depois** em JSON.

A senha (hash) nunca é gravada. A aba *Auditoria* (só ADMIN) filtra por tabela, operação e texto, e mostra
os campos alterados em cada UPDATE.

## Índices e restrições (V4)

- **CHECK**:
  - preço > 0, ano entre 1000 e 2100, páginas > 0;
  - estoque ≥ 0;
  - quantidade e valor do item > 0, e `subtotal = quantidade × valor_unitario`;
  - total ≥ 0, pagamento > 0;
  - formatos de CPF, UF, CEP e CNPJ;
  - e-mail em minúsculas;
  - status de pedido e pagamento.
- **UNIQUE**: (pedido, livro) no item do pedido, e CNPJ da editora (quando informado).
- **Índices** nas chaves estrangeiras e nas colunas de busca: cliente, status e data do pedido, itens, pagamentos, autor, tabela/data da auditoria.

## Como executar (Windows)

Requisitos: JDK 27 e PostgreSQL 17 (local ou `docker compose up -d postgres`).

1. Banco `livraria`, usuário `livraria`, senha `livraria_dev`. Para alterar, use `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`. O Flyway cria e atualiza o esquema sozinho.
2. Inicie a aplicação: `.\mvnw.cmd spring-boot:run` (ou F5 no VS Code).
3. Abra **http://localhost:8080** e entre com `admin@livraria.com` / `admin123`.
   Para testar o perfil VENDEDOR, crie um usuário com esse perfil na aba *Usuarios*.

Documentação da API (Swagger): http://localhost:8080/swagger-ui.html

## Endpoints principais

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/auth/login`, `/api/auth/registro` | Login (retorna JWT) e auto-cadastro de cliente |
| GET/POST/PUT/DELETE | `/api/{livros,autores,editoras,categorias,clientes,estoque,usuarios}[/{id}]` | CRUDs (`?q=` busca) |
| POST | `/api/estoque` | Entrada de estoque: `{"livroId", "quantidade", "estoqueMinimo"?}` soma ao saldo |
| PUT | `/api/estoque/{id}` | Altera só o estoque mínimo: `{"estoqueMinimo"}` |
| GET | `/api/estoque?baixo=true` | Livros abaixo do estoque mínimo |
| GET/POST | `/api/pedidos` (`?status=&q=`) | Lista / cria pedido com itens |
| GET/PUT/DELETE | `/api/pedidos/{id}` | Detalhe (itens + pagamentos) / altera itens / exclui |
| PUT | `/api/pedidos/{id}/status` | `{"status": "ENVIADO" \| "ENTREGUE" \| "CANCELADO"}` |
| POST | `/api/pedidos/{id}/pagamento` | Pagamento pelo próprio comprador: `{"formaPagamento": "PIX" \| "CARTAO_CREDITO" \| "CARTAO_DEBITO" \| "BOLETO"}` |
| GET/POST | `/api/pagamentos` (`?status=&q=`) | Lista / registra pagamento |
| PUT | `/api/pagamentos/{id}/status` | `{"status": "APROVADO" \| "CANCELADO" \| "ESTORNADO"}` |
| GET | `/api/auditoria` (`?tabela=&operacao=&q=&limite=`), `/api/auditoria/tabelas` | Consulta da auditoria |

Erros seguem o formato `{"status": 409, "mensagem": "...", "campos": {"campo": "erro"}, "timestamp": "..."}`.

## Testes

Os testes usam um banco separado, **`livraria_test`** (crie uma vez):

```powershell
psql -U livraria -d livraria -c "CREATE DATABASE livraria_test OWNER livraria"
```

```powershell
.\mvnw.cmd test                                         # todos (cerca de 3 minutos)
.\mvnw.cmd test "-DexcludedGroups=e2e"                  # unitários + integração (rápido)
.\mvnw.cmd test "-Dgroups=e2e"                          # apenas E2E (navegador)
.\mvnw.cmd test "-Dgroups=e2e" "-De2e.headless=false"   # E2E com o Chrome visível
```

| Nível | Pacote | Testes | O que cobre |
|---|---|---|---|
| Unitário (JUnit 5 + Mockito, sem Spring) | `unit` | 71 | Regras de pedido, pagamento, estoque (entrada soma ao saldo), usuário, autor, livro, login e auto-cadastro, JWT, validação de CPF e de imagens |
| Integração (SpringBootTest + MockMvc + PostgreSQL real) | `integration` | 63 | Fluxo de venda completo pela API, pagamento pelo cliente (só o próprio pedido), estoque (entrada e edição só do mínimo), permissões dos 3 perfis, CRUDs, cliente duplicado por CPF/e-mail nos dois sentidos, upload/leitura/remoção de imagens com seus metadados, validações (400), conflitos (409), auditoria gravada pelos triggers e recusa de SQL que viola as restrições do banco |
| Ponta a ponta / E2E (Selenium WebDriver + Chrome) | `e2e` | 19 | Pelas telas reais: login, cadastro com CPF, CRUDs (com upload de capa e foto), venda completa pelo vendedor (cliente → pedido → pagamento → entrega), cliente comprando e pagando, cliente comprando e cancelando, estoque insuficiente, catálogo e consulta da auditoria |
| Carga do contexto | raiz | 1 | A aplicação sobe com todas as configurações |

- Os testes de integração rodam em transação desfeita ao final. A exceção é a auditoria, que limpa os próprios dados.
- Os E2E criam dados com prefixo `E2E`/`e2e.` e os removem após cada teste, devolvendo ao estoque o que venderam.
- CPFs e e-mails dos testes são gerados únicos e válidos (`DadosTeste`), pois o sistema não aceita cliente duplicado.
- Os E2E exigem o **Google Chrome**. Na primeira execução, o Selenium Manager baixa o ChromeDriver automaticamente. Sem Chrome, os E2E são ignorados.
