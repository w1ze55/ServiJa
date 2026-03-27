-- V22__create_freelancer_wallet_system.sql

-- Tabela de carteira dos freelancers
CREATE TABLE freelancer_wallets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    available_balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    pending_balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_earned DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_withdrawn DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_available_balance (available_balance)
);

-- Tabela de solicitações de saque
CREATE TABLE withdrawal_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    freelancer_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    pix_key VARCHAR(255) NOT NULL,
    pix_key_type VARCHAR(50),
    asaas_transfer_id VARCHAR(255),
    transaction_id VARCHAR(255),
    rejection_reason TEXT,
    processed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (freelancer_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_freelancer_id (freelancer_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_asaas_transfer_id (asaas_transfer_id)
);

-- Atualizar tabela de pagamentos para rastrear liberação para freelancer
ALTER TABLE payments
ADD COLUMN released_to_freelancer BOOLEAN DEFAULT FALSE,
ADD COLUMN released_at DATETIME,
ADD COLUMN release_days INT DEFAULT 7 COMMENT 'Dias após pagamento para liberar ao freelancer';
