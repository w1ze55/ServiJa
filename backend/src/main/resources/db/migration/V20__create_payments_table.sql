-- V20__create_payments_table.sql

CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_request_id BIGINT NOT NULL,
    contractor_id BIGINT NOT NULL,
    freelancer_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    platform_fee DECIMAL(10, 2) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    transaction_id VARCHAR(255),
    pix_qr_code TEXT,
    pix_copy_paste TEXT,
    card_last_digits VARCHAR(4),
    refund_reason TEXT,
    completed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (service_request_id) REFERENCES service_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (contractor_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (freelancer_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_contractor_id (contractor_id),
    INDEX idx_freelancer_id (freelancer_id),
    INDEX idx_service_request_id (service_request_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);
