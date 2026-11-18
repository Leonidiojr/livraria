-- Esquema inicial do Sistema de Vendas de Livros.
-- Usa IF NOT EXISTS para ser compativel com bancos em que o esquema ja foi criado manualmente.

CREATE SCHEMA IF NOT EXISTS livraria;
SET search_path TO livraria;

CREATE TABLE IF NOT EXISTS perfil (
    id_perfil  SERIAL PRIMARY KEY,
    nome       VARCHAR(50)  NOT NULL UNIQUE,
    descricao  VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS usuario (
    id_usuario     SERIAL PRIMARY KEY,
    nome           VARCHAR(150) NOT NULL,
    email          VARCHAR(150) NOT NULL UNIQUE,
    senha_hash     VARCHAR(255) NOT NULL,
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
    data_cadastro  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acesso  TIMESTAMP,
    id_perfil      INTEGER      NOT NULL,
    CONSTRAINT fk_usuario_perfil FOREIGN KEY (id_perfil) REFERENCES perfil (id_perfil)
);
CREATE INDEX IF NOT EXISTS idx_usuario_email ON usuario (email);

CREATE TABLE IF NOT EXISTS categoria (
    id_categoria SERIAL PRIMARY KEY,
    nome         VARCHAR(100) NOT NULL UNIQUE,
    descricao    VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS editora (
    id_editora SERIAL PRIMARY KEY,
    nome       VARCHAR(150) NOT NULL UNIQUE,
    cnpj       VARCHAR(18),
    telefone   VARCHAR(20),
    email      VARCHAR(150),
    site       VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS autor (
    id_autor        SERIAL PRIMARY KEY,
    nome            VARCHAR(150) NOT NULL,
    nacionalidade   VARCHAR(100),
    data_nascimento DATE,
    biografia       TEXT
);

CREATE TABLE IF NOT EXISTS livro (
    id_livro       SERIAL PRIMARY KEY,
    isbn           VARCHAR(20)    NOT NULL UNIQUE,
    titulo         VARCHAR(250)   NOT NULL,
    subtitulo      VARCHAR(250),
    descricao      TEXT,
    ano_publicacao INTEGER,
    numero_paginas INTEGER,
    idioma         VARCHAR(50),
    preco          NUMERIC(10, 2) NOT NULL,
    id_categoria   INTEGER        NOT NULL,
    id_editora     INTEGER        NOT NULL,
    ativo          BOOLEAN   DEFAULT TRUE,
    data_cadastro  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_livro_categoria FOREIGN KEY (id_categoria) REFERENCES categoria (id_categoria),
    CONSTRAINT fk_livro_editora FOREIGN KEY (id_editora) REFERENCES editora (id_editora)
);
CREATE INDEX IF NOT EXISTS idx_livro_categoria ON livro (id_categoria);
CREATE INDEX IF NOT EXISTS idx_livro_editora ON livro (id_editora);
CREATE INDEX IF NOT EXISTS idx_livro_isbn ON livro (isbn);
CREATE INDEX IF NOT EXISTS idx_livro_titulo ON livro (titulo);

CREATE TABLE IF NOT EXISTS livro_autor (
    id_livro INTEGER NOT NULL,
    id_autor INTEGER NOT NULL,
    PRIMARY KEY (id_livro, id_autor),
    CONSTRAINT fk_la_livro FOREIGN KEY (id_livro) REFERENCES livro (id_livro) ON DELETE CASCADE,
    CONSTRAINT fk_la_autor FOREIGN KEY (id_autor) REFERENCES autor (id_autor) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS estoque (
    id_estoque         SERIAL PRIMARY KEY,
    id_livro           INTEGER NOT NULL UNIQUE,
    quantidade         INTEGER NOT NULL DEFAULT 0,
    estoque_minimo     INTEGER   DEFAULT 5,
    ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_estoque_livro FOREIGN KEY (id_livro) REFERENCES livro (id_livro)
);

CREATE TABLE IF NOT EXISTS cliente (
    id_cliente      SERIAL PRIMARY KEY,
    id_usuario      INTEGER UNIQUE,
    cpf             VARCHAR(14) UNIQUE,
    telefone        VARCHAR(20),
    endereco        VARCHAR(255),
    numero          VARCHAR(20),
    complemento     VARCHAR(100),
    bairro          VARCHAR(100),
    cidade          VARCHAR(100),
    estado          CHAR(2),
    cep             VARCHAR(10),
    data_nascimento DATE,
    CONSTRAINT fk_cliente_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);
CREATE INDEX IF NOT EXISTS idx_cliente_cpf ON cliente (cpf);

CREATE TABLE IF NOT EXISTS pedido (
    id_pedido   SERIAL PRIMARY KEY,
    id_cliente  INTEGER     NOT NULL,
    data_pedido TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valor_total NUMERIC(12, 2) DEFAULT 0,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    observacoes TEXT,
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    CONSTRAINT chk_status_pedido CHECK (status IN ('PENDENTE', 'PAGO', 'ENVIADO', 'ENTREGUE', 'CANCELADO'))
);
CREATE INDEX IF NOT EXISTS idx_pedido_data ON pedido (data_pedido);

CREATE TABLE IF NOT EXISTS item_pedido (
    id_item        SERIAL PRIMARY KEY,
    id_pedido      INTEGER        NOT NULL,
    id_livro       INTEGER        NOT NULL,
    quantidade     INTEGER        NOT NULL,
    valor_unitario NUMERIC(10, 2) NOT NULL,
    subtotal       NUMERIC(12, 2) NOT NULL,
    CONSTRAINT fk_item_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_item_livro FOREIGN KEY (id_livro) REFERENCES livro (id_livro)
);

CREATE TABLE IF NOT EXISTS pagamento (
    id_pagamento     SERIAL PRIMARY KEY,
    id_pedido        INTEGER        NOT NULL,
    data_pagamento   TIMESTAMP,
    valor            NUMERIC(12, 2) NOT NULL,
    forma_pagamento  VARCHAR(30)    NOT NULL,
    status           VARCHAR(20) DEFAULT 'PENDENTE',
    codigo_transacao VARCHAR(100),
    CONSTRAINT fk_pagamento_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido),
    CONSTRAINT chk_forma_pagamento CHECK (forma_pagamento IN ('PIX', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'BOLETO')),
    CONSTRAINT chk_status_pagamento CHECK (status IN ('PENDENTE', 'APROVADO', 'CANCELADO', 'ESTORNADO'))
);
CREATE INDEX IF NOT EXISTS idx_pagamento_status ON pagamento (status);

CREATE TABLE IF NOT EXISTS auditoria (
    id_auditoria BIGSERIAL PRIMARY KEY,
    tabela       VARCHAR(100) NOT NULL,
    operacao     VARCHAR(20)  NOT NULL,
    usuario_bd   VARCHAR(100),
    data_evento  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    detalhes     TEXT
);
