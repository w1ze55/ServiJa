ALTER TABLE payments
ADD COLUMN platform_fee_released_at TIMESTAMP NULL AFTER completed_at;

CREATE TABLE IF NOT EXISTS platform_wallets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    available_balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    reserved_balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_earned DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_withdrawn DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS platform_bank_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_type VARCHAR(20) NOT NULL DEFAULT 'PIX_KEY',
    bank_name VARCHAR(100),
    bank_code VARCHAR(10),
    agency VARCHAR(10),
    agency_digit VARCHAR(2),
    account_number VARCHAR(20),
    account_digit VARCHAR(2),
    account_bank_type VARCHAR(20),
    pix_key VARCHAR(255),
    pix_key_type VARCHAR(20),
    holder_name VARCHAR(255) NOT NULL,
    holder_cpf_cnpj VARCHAR(20) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_platform_bank_accounts_default (is_default),
    INDEX idx_platform_bank_accounts_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS platform_withdrawal_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    withdrawal_method VARCHAR(20) NOT NULL,
    bank_account_id BIGINT NOT NULL,
    bank_account_type VARCHAR(20) NOT NULL,
    bank_name VARCHAR(100),
    bank_code VARCHAR(10),
    agency VARCHAR(10),
    agency_digit VARCHAR(2),
    account_number VARCHAR(20),
    account_digit VARCHAR(2),
    account_bank_type VARCHAR(20),
    pix_key VARCHAR(255),
    pix_key_type VARCHAR(20),
    holder_name VARCHAR(255) NOT NULL,
    holder_cpf_cnpj VARCHAR(20) NOT NULL,
    destination_label VARCHAR(255) NOT NULL,
    asaas_transfer_id VARCHAR(255),
    transaction_id VARCHAR(255) NOT NULL,
    rejection_reason TEXT,
    processed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_platform_withdrawals_status (status),
    INDEX idx_platform_withdrawals_created_at (created_at),
    INDEX idx_platform_withdrawals_bank_account_id (bank_account_id),
    INDEX idx_platform_withdrawals_transaction_id (transaction_id),
    INDEX idx_platform_withdrawals_asaas_transfer_id (asaas_transfer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

UPDATE payments p
JOIN service_requests sr ON sr.id = p.service_request_id
SET p.platform_fee_released_at = COALESCE(sr.updated_at, p.completed_at, p.updated_at, p.created_at)
WHERE sr.status = 'COMPLETED'
  AND p.platform_fee_released_at IS NULL;

INSERT INTO platform_wallets (
    id,
    available_balance,
    reserved_balance,
    total_earned,
    total_withdrawn
)
SELECT
    1,
    COALESCE(SUM(CASE WHEN p.platform_fee_released_at IS NOT NULL THEN p.platform_fee ELSE 0 END), 0),
    0,
    COALESCE(SUM(CASE WHEN p.platform_fee_released_at IS NOT NULL THEN p.platform_fee ELSE 0 END), 0),
    0
FROM payments p
ON DUPLICATE KEY UPDATE
    available_balance = VALUES(available_balance),
    reserved_balance = VALUES(reserved_balance),
    total_earned = VALUES(total_earned),
    total_withdrawn = VALUES(total_withdrawn);

CREATE INDEX idx_payments_platform_fee_released_at
ON payments(platform_fee_released_at);
