-- Soft delete de serviços: marca serviços excluídos que possuem solicitações associadas
-- (preserva o registro para o histórico de transações). NULL = serviço ativo/não excluído.
ALTER TABLE services ADD COLUMN deleted_at DATETIME NULL;
