-- Completa as tabelas de apoio para que toda entidade tenha ao menos 5 atributos.

-- perfil: situacao e data de cadastro
ALTER TABLE perfil ADD COLUMN IF NOT EXISTS ativo         BOOLEAN   NOT NULL DEFAULT TRUE;
ALTER TABLE perfil ADD COLUMN IF NOT EXISTS data_cadastro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- capa do livro e foto do autor: nome do arquivo enviado, tamanho e data do envio
ALTER TABLE livro_capa ADD COLUMN IF NOT EXISTS nome_arquivo  VARCHAR(255);
ALTER TABLE livro_capa ADD COLUMN IF NOT EXISTS tamanho_bytes INTEGER;
ALTER TABLE livro_capa ADD COLUMN IF NOT EXISTS data_envio    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
UPDATE livro_capa SET tamanho_bytes = octet_length(conteudo) WHERE tamanho_bytes IS NULL;
ALTER TABLE livro_capa ALTER COLUMN tamanho_bytes SET NOT NULL;
ALTER TABLE livro_capa ADD CONSTRAINT chk_capa_tamanho_bytes CHECK (tamanho_bytes = octet_length(conteudo));

ALTER TABLE autor_foto ADD COLUMN IF NOT EXISTS nome_arquivo  VARCHAR(255);
ALTER TABLE autor_foto ADD COLUMN IF NOT EXISTS tamanho_bytes INTEGER;
ALTER TABLE autor_foto ADD COLUMN IF NOT EXISTS data_envio    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
UPDATE autor_foto SET tamanho_bytes = octet_length(conteudo) WHERE tamanho_bytes IS NULL;
ALTER TABLE autor_foto ALTER COLUMN tamanho_bytes SET NOT NULL;
ALTER TABLE autor_foto ADD CONSTRAINT chk_foto_tamanho_bytes CHECK (tamanho_bytes = octet_length(conteudo));
