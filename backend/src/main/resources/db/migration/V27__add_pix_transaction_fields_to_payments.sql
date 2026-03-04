-- Adiciona campos para rastrear transações PIX recebidas via webhook
-- Esses campos armazenam informações retornadas pelo Asaas quando um PIX é confirmado

-- ID da transação PIX no Asaas (diferente do asaas_payment_id que é da cobrança)
ALTER TABLE payments ADD COLUMN pix_transaction_id VARCHAR(255) COMMENT 'ID da transação PIX no Asaas';

-- Identificador End-to-End do PIX (único por transação no sistema bancário)
ALTER TABLE payments ADD COLUMN pix_end_to_end_id VARCHAR(255) COMMENT 'Identificador End-to-End do PIX (único no sistema bancário)';

-- Status da transação PIX (DONE, CANCELLED, AWAITING_CRITICAL_ACTION_AUTHORIZATION, etc.)
ALTER TABLE payments ADD COLUMN pix_transaction_status VARCHAR(100) COMMENT 'Status da transação PIX: DONE, CANCELLED, AWAITING_CRITICAL_ACTION_AUTHORIZATION, etc.';

-- Índice para busca por transação PIX
CREATE INDEX idx_payments_pix_transaction_id ON payments(pix_transaction_id);
CREATE INDEX idx_payments_pix_end_to_end_id ON payments(pix_end_to_end_id);
