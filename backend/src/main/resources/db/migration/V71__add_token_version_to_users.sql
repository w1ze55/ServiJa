-- H6: versão de credenciais para revogação de JWT stateless.
-- Incrementada em reset/troca de senha; o filtro compara o claim `tv` do token
-- com este valor e rejeita tokens emitidos antes do incremento.
ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0;
