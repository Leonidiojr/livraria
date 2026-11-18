-- Impede cadastro de cliente em duplicidade (mesmo CPF ou mesmo e-mail), seja pela equipe ou pelo auto-cadastro.
-- A aplicacao ja verifica antes de gravar; os indices garantem a regra tambem no banco.
CREATE UNIQUE INDEX IF NOT EXISTS ux_cliente_cpf   ON cliente (cpf)          WHERE cpf IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_cliente_email ON cliente (lower(email)) WHERE email IS NOT NULL;
