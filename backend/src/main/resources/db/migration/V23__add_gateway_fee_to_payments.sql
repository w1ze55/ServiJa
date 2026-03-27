-- V23: Adicionar campo gateway_fee à tabela payments
ALTER TABLE payments ADD COLUMN gateway_fee DECIMAL(10, 2) DEFAULT 0.00 COMMENT 'Taxa cobrada pelo gateway de pagamento (Asaas)';
