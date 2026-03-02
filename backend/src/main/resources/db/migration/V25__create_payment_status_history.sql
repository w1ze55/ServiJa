-- Tabela para histórico de status de pagamento
CREATE TABLE payment_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    asaas_event VARCHAR(100),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    INDEX idx_payment_id (payment_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Adicionar campo para controlar se o chat está habilitado
ALTER TABLE service_requests
ADD COLUMN chat_enabled BOOLEAN DEFAULT FALSE AFTER status;

-- Atualizar solicitações já pagas para habilitar o chat
UPDATE service_requests sr
SET sr.chat_enabled = TRUE
WHERE EXISTS (
    SELECT 1 
    FROM payments p 
    WHERE p.service_request_id = sr.id 
    AND p.status = 'COMPLETED'
);
