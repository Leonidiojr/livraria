-- Foto da capa do livro e foto do autor.
-- As imagens ficam em tabelas proprias (1:1) para que listar livros/autores nao carregue os bytes.
-- A coluna *_atualizada_em indica se existe imagem e serve de versao na URL (cache do navegador).
-- Estas tabelas nao tem trigger de auditoria (o JSON com a imagem seria enorme); a troca da imagem
-- fica registrada pela alteracao de capa_atualizada_em / foto_atualizada_em.
SET search_path TO livraria;

ALTER TABLE livro ADD COLUMN IF NOT EXISTS capa_atualizada_em TIMESTAMP;
ALTER TABLE autor ADD COLUMN IF NOT EXISTS foto_atualizada_em TIMESTAMP;

CREATE TABLE IF NOT EXISTS livro_capa (
    id_livro INTEGER PRIMARY KEY,
    conteudo BYTEA        NOT NULL,
    tipo     VARCHAR(30)  NOT NULL,
    CONSTRAINT fk_capa_livro FOREIGN KEY (id_livro) REFERENCES livro (id_livro) ON DELETE CASCADE,
    CONSTRAINT chk_capa_tipo CHECK (tipo IN ('image/jpeg', 'image/png', 'image/webp')),
    CONSTRAINT chk_capa_tamanho CHECK (octet_length(conteudo) BETWEEN 1 AND 2097152)
);

CREATE TABLE IF NOT EXISTS autor_foto (
    id_autor INTEGER PRIMARY KEY,
    conteudo BYTEA        NOT NULL,
    tipo     VARCHAR(30)  NOT NULL,
    CONSTRAINT fk_foto_autor FOREIGN KEY (id_autor) REFERENCES autor (id_autor) ON DELETE CASCADE,
    CONSTRAINT chk_foto_tipo CHECK (tipo IN ('image/jpeg', 'image/png', 'image/webp')),
    CONSTRAINT chk_foto_tamanho CHECK (octet_length(conteudo) BETWEEN 1 AND 2097152)
);
