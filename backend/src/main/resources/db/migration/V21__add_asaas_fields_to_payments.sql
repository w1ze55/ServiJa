-- V21__add_asaas_fields_to_payments.sql

ALTER TABLE payments
ADD COLUMN asaas_payment_id VARCHAR(255) AFTER transaction_id,
ADD COLUMN asaas_customer_id VARCHAR(255) AFTER asaas_payment_id;

CREATE INDEX idx_asaas_payment_id ON payments(asaas_payment_id);
CREATE INDEX idx_asaas_customer_id ON payments(asaas_customer_id);
