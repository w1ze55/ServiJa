-- Adicionar novos status ao enum de Payment
-- CANCELLED, OVERDUE, CHARGEBACK

-- MySQL: Alterar a coluna status para suportar os novos valores
ALTER TABLE payments 
MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
