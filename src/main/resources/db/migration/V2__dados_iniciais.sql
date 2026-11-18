-- Dados de apoio. O usuario administrador e criado pela aplicacao na inicializacao (senha com BCrypt).
SET search_path TO livraria;

-- Categoria passa a ter 5 atributos (id, nome, descricao, ativo, data_cadastro)
ALTER TABLE categoria ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE categoria ADD COLUMN IF NOT EXISTS data_cadastro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

INSERT INTO perfil (nome, descricao) VALUES
    ('ADMIN', 'Administrador do sistema'),
    ('CLIENTE', 'Cliente da livraria')
ON CONFLICT (nome) DO NOTHING;

INSERT INTO categoria (nome, descricao) VALUES
    ('Romance', 'Ficcao narrativa em prosa'),
    ('Fantasia', 'Mundos e elementos fantasticos'),
    ('Tecnologia', 'Computacao, programacao e engenharia'),
    ('Historia', 'Obras historicas e biografias'),
    ('Infantil', 'Livros para criancas')
ON CONFLICT (nome) DO NOTHING;

INSERT INTO editora (nome, cnpj, telefone, email, site) VALUES
    ('Companhia das Letras', '55.789.390/0001-12', '(11) 3707-3500', 'contato@companhiadasletras.com.br', 'https://www.companhiadasletras.com.br'),
    ('Novatec', '04.205.932/0001-84', '(11) 2959-6529', 'novatec@novatec.com.br', 'https://novatec.com.br'),
    ('Rocco', '42.444.703/0001-59', '(21) 3525-2000', 'rocco@rocco.com.br', 'https://www.rocco.com.br')
ON CONFLICT (nome) DO NOTHING;
