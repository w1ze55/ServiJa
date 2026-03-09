-- Criar tabela de ajustes de entrega
CREATE TABLE delivery_adjustments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_request_id BIGINT NOT NULL,
    requested_by_user_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    freelancer_response TEXT,
    responded_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_adjustment_service_request 
        FOREIGN KEY (service_request_id) 
        REFERENCES service_requests(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_adjustment_requested_by 
        FOREIGN KEY (requested_by_user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE,
    
    INDEX idx_service_request (service_request_id),
    INDEX idx_requested_by (requested_by_user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
