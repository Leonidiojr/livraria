# Páginas Vivas — App Android (Java)

Front-end mobile nativo em **Java** para a API REST da livraria (pasta raiz deste repositório).
Consome os mesmos endpoints do front-end web, com autenticação JWT.

## Funcionalidades

| Tela | O que faz | API |
|---|---|---|
| Login / Criar conta | Entrar com e-mail e senha; auto-cadastro de cliente (nome, e-mail, CPF com máscara, senha); engrenagem para trocar o endereço do servidor | `POST /api/auth/login`, `POST /api/auth/registro` |
| Catálogo | Lista de livros com capa (ou iniciais), autores, preço e estoque; busca com atraso de digitação; puxar para atualizar | `GET /api/livros?q=` |
| Detalhes do livro | Capa, preço, estoque, ficha técnica e sinopse; **Adicionar ao carrinho** e **Comprar agora** | `GET /api/livros/{id}`, `GET /api/livros/{id}/capa` |
| Carrinho | Alterar quantidades (limitadas ao estoque), remover, observações e **Finalizar pedido**. ADMIN/VENDEDOR escolhem o cliente | `GET /api/clientes`, `POST /api/pedidos` |
| Pedidos | Lista de pedidos (cliente vê só os próprios) | `GET /api/pedidos` |
| Detalhe do pedido | Itens, pagamentos, saldo; **Pagar** (PIX, cartões, boleto); cancelar; ADMIN/VENDEDOR mudam o status (enviado, entregue) | `GET /api/pedidos/{id}`, `POST /api/pedidos/{id}/pagamento`, `PUT /api/pedidos/{id}/status` |

Logo após finalizar um pedido, o app abre o pedido e já oferece o pagamento, como no front-end web.
Se o token expirar (401), o app volta para o login. As mensagens de erro da API (`mensagem` e `campos`) são mostradas ao usuário.

## Tecnologias

- Java 17 (código-fonte), Android SDK 37 (`compileSdk`/`targetSdk`), `minSdk` 24 (Android 7.0+)
- Android Gradle Plugin 9.5.1, Gradle 9.9.1 (wrapper incluído)
- AndroidX AppCompat, Material Components 3, RecyclerView, SwipeRefreshLayout, View Binding
- Retrofit 2 + Gson (HTTP/JSON), OkHttp (interceptor que envia `Authorization: Bearer <token>`)
- Glide (capas dos livros)

## Estrutura

```
app/src/main/java/com/example/livraria/android/
├── api/    LivrariaApi (endpoints Retrofit), ApiClient (Retrofit + JWT), Modelos (DTOs), Resposta (callback + erros)
├── data/   Sessao (token, perfil, URL do servidor), Carrinho (em memória)
├── ui/     LoginActivity, MainActivity (abas), CatalogoFragment, LivroDetalheActivity,
│           CarrinhoFragment, PedidosFragment, PedidoDetalheActivity, MascaraCpf
└── util/   Formato (moeda R$, datas, status, capas)
```

## Como executar

1. Suba a API (na raiz do repositório): `.\mvnw.cmd spring-boot:run`.
2. Abra a pasta **`android/`** no **Android Studio** (2026.2 ou mais novo) e rode no emulador.
   Pela linha de comando: `.\gradlew.bat assembleDebug` (com `JAVA_HOME` apontando para o JBR do Android Studio).
3. O endereço padrão da API é `http://10.0.2.2:8080/`, que é o `localhost` do computador visto pelo emulador.
4. Em um **celular físico**, toque na engrenagem da tela de login e informe `http://<IP-do-computador>:8080/`
   (o celular e o computador precisam estar na mesma rede, e o firewall do Windows deve liberar a porta 8080).
5. Entre com `admin@livraria.com` / `admin123`, ou crie uma conta de cliente pelo próprio app.

> **Android 17+:** na primeira abertura o app pede a permissão **acesso à rede local**
> (`ACCESS_LOCAL_NETWORK`). Sem ela, o Android bloqueia a conexão com `10.0.2.2`/`192.168.x.x` e o login termina em timeout.

## Observações

- O tráfego HTTP sem TLS está liberado (`res/xml/network_security_config.xml`) apenas para desenvolvimento.
  Em produção, publique a API em HTTPS e remova essa permissão.
- O preço no carrinho é uma prévia: o total oficial é calculado pelo servidor ao criar o pedido.
- O CRUD administrativo (livros, autores, usuários, estoque, auditoria) continua no front-end web.
  O app foca na experiência de compra e no acompanhamento de pedidos.
