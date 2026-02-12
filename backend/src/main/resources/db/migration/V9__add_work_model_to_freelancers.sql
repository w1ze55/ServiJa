-- Adiciona campo workModel à tabela freelancers
ALTER TABLE freelancers ADD COLUMN work_model VARCHAR(20);

-- Atualiza valores existentes para PRESENCIAL por padrão
UPDATE freelancers SET work_model = 'PRESENCIAL' WHERE work_model IS NULL;
