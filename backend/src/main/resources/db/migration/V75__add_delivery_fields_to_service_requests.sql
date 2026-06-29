-- Campos da entrega do freelancer (descrição + anexos) registrados ao marcar como DELIVERED.
ALTER TABLE service_requests
    ADD COLUMN delivery_description TEXT NULL,
    ADD COLUMN delivery_attachments TEXT NULL,
    ADD COLUMN delivered_at DATETIME NULL;
