-- Expansao: perfil VENDEDOR, clientes com nome/e-mail, estoque inicial, restricoes, indices e auditoria por trigger.
SET search_path TO livraria;

-- ================================================================ perfis
INSERT INTO perfil (nome, descricao) VALUES ('VENDEDOR', 'Vendedor da livraria')
ON CONFLICT (nome) DO NOTHING;

-- ================================================================ cliente: nome e e-mail proprios
-- (o cliente pode existir sem usuario de acesso; quando tiver, id_usuario faz o vinculo)
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS nome  VARCHAR(150);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS email VARCHAR(150);
UPDATE cliente c SET nome = u.nome, email = u.email
FROM usuario u WHERE c.id_usuario = u.id_usuario AND c.nome IS NULL;
UPDATE cliente SET nome = 'Cliente ' || id_cliente WHERE nome IS NULL;
ALTER TABLE cliente ALTER COLUMN nome SET NOT NULL;
ALTER TABLE cliente ALTER COLUMN estado TYPE VARCHAR(2);

-- ================================================================ estoque inicial para livros sem registro
INSERT INTO estoque (id_livro, quantidade, estoque_minimo)
SELECT l.id_livro, 5 + (l.id_livro * 7) % 40, 5
FROM livro l
WHERE NOT EXISTS (SELECT 1 FROM estoque e WHERE e.id_livro = l.id_livro);

-- ================================================================ restricoes (CHECK / UNIQUE)
ALTER TABLE livro       ADD CONSTRAINT chk_livro_preco        CHECK (preco > 0);
ALTER TABLE livro       ADD CONSTRAINT chk_livro_ano          CHECK (ano_publicacao IS NULL OR ano_publicacao BETWEEN 1000 AND 2100);
ALTER TABLE livro       ADD CONSTRAINT chk_livro_paginas      CHECK (numero_paginas IS NULL OR numero_paginas > 0);
ALTER TABLE estoque     ADD CONSTRAINT chk_estoque_quantidade CHECK (quantidade >= 0);
ALTER TABLE estoque     ADD CONSTRAINT chk_estoque_minimo     CHECK (estoque_minimo IS NULL OR estoque_minimo >= 0);
ALTER TABLE item_pedido ADD CONSTRAINT chk_item_quantidade    CHECK (quantidade > 0);
ALTER TABLE item_pedido ADD CONSTRAINT chk_item_valor         CHECK (valor_unitario > 0);
ALTER TABLE item_pedido ADD CONSTRAINT chk_item_subtotal      CHECK (subtotal = quantidade * valor_unitario);
ALTER TABLE item_pedido ADD CONSTRAINT uk_item_pedido_livro   UNIQUE (id_pedido, id_livro);
ALTER TABLE pedido      ADD CONSTRAINT chk_pedido_total       CHECK (valor_total IS NULL OR valor_total >= 0);
ALTER TABLE pagamento   ADD CONSTRAINT chk_pagamento_valor    CHECK (valor > 0);
ALTER TABLE cliente     ADD CONSTRAINT chk_cliente_cpf        CHECK (cpf IS NULL OR cpf ~ '^[0-9]{3}\.[0-9]{3}\.[0-9]{3}-[0-9]{2}$');
ALTER TABLE cliente     ADD CONSTRAINT chk_cliente_uf         CHECK (estado IS NULL OR estado ~ '^[A-Z]{2}$');
ALTER TABLE cliente     ADD CONSTRAINT chk_cliente_cep        CHECK (cep IS NULL OR cep ~ '^[0-9]{5}-[0-9]{3}$');
ALTER TABLE usuario     ADD CONSTRAINT chk_usuario_email      CHECK (email = lower(email));
ALTER TABLE editora     ADD CONSTRAINT chk_editora_cnpj       CHECK (cnpj IS NULL OR cnpj ~ '^[0-9]{2}\.[0-9]{3}\.[0-9]{3}/[0-9]{4}-[0-9]{2}$');
CREATE UNIQUE INDEX IF NOT EXISTS uk_editora_cnpj ON editora (cnpj) WHERE cnpj IS NOT NULL;

-- ================================================================ indices para consultas e chaves estrangeiras
CREATE INDEX IF NOT EXISTS idx_usuario_perfil      ON usuario (id_perfil);
CREATE INDEX IF NOT EXISTS idx_autor_nome          ON autor (nome);
CREATE INDEX IF NOT EXISTS idx_livro_autor_autor   ON livro_autor (id_autor);
CREATE INDEX IF NOT EXISTS idx_cliente_nome        ON cliente (nome);
CREATE INDEX IF NOT EXISTS idx_pedido_cliente      ON pedido (id_cliente);
CREATE INDEX IF NOT EXISTS idx_pedido_status       ON pedido (status);
CREATE INDEX IF NOT EXISTS idx_item_pedido_pedido  ON item_pedido (id_pedido);
CREATE INDEX IF NOT EXISTS idx_item_pedido_livro   ON item_pedido (id_livro);
CREATE INDEX IF NOT EXISTS idx_pagamento_pedido    ON pagamento (id_pedido);
CREATE INDEX IF NOT EXISTS idx_auditoria_tabela    ON auditoria (tabela, data_evento DESC);
CREATE INDEX IF NOT EXISTS idx_auditoria_data      ON auditoria (data_evento DESC);

-- ================================================================ auditoria basica
-- Cada INSERT/UPDATE/DELETE nas tabelas de negocio grava uma linha em "auditoria" com o JSON
-- do registro antes/depois. O usuario e lido da variavel de sessao app.usuario (definida pela
-- aplicacao com o e-mail do usuario logado); sem ela, usa o usuario do banco. A senha nunca e gravada.
CREATE OR REPLACE FUNCTION fn_auditoria() RETURNS trigger AS $$
DECLARE
    v_usuario TEXT := COALESCE(NULLIF(current_setting('app.usuario', true), ''), current_user);
    v_dados   JSONB;
BEGIN
    IF TG_OP = 'INSERT' THEN
        v_dados := jsonb_build_object('depois', to_jsonb(NEW) - 'senha_hash');
    ELSIF TG_OP = 'UPDATE' THEN
        v_dados := jsonb_build_object('antes', to_jsonb(OLD) - 'senha_hash', 'depois', to_jsonb(NEW) - 'senha_hash');
    ELSE
        v_dados := jsonb_build_object('antes', to_jsonb(OLD) - 'senha_hash');
    END IF;
    INSERT INTO livraria.auditoria (tabela, operacao, usuario_bd, data_evento, detalhes)
    VALUES (TG_TABLE_NAME, TG_OP, v_usuario, CURRENT_TIMESTAMP, v_dados::TEXT);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['usuario', 'autor', 'editora', 'categoria', 'livro', 'estoque',
                             'cliente', 'pedido', 'item_pedido', 'pagamento']
    LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS trg_auditoria_%1$s ON livraria.%1$s', t);
        EXECUTE format('CREATE TRIGGER trg_auditoria_%1$s AFTER INSERT OR UPDATE OR DELETE ON livraria.%1$s
                        FOR EACH ROW EXECUTE FUNCTION livraria.fn_auditoria()', t);
    END LOOP;
END $$;
