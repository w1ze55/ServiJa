-- Tabela de contas bancárias / chaves PIX do freelancer
CREATE TABLE IF NOT EXISTS freelancer_bank_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
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
    CONSTRAINT fk_bank_account_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Adicionar campo de método de saque na tabela de withdrawal_requests
ALTER TABLE withdrawal_requests ADD COLUMN withdrawal_method VARCHAR(20) NOT NULL DEFAULT 'PIX' AFTER pix_key_type;

-- Adicionar campos de conta bancária para saque via TED
ALTER TABLE withdrawal_requests ADD COLUMN bank_account_id BIGINT AFTER withdrawal_method;
ALTER TABLE withdrawal_requests ADD CONSTRAINT fk_withdrawal_bank_account FOREIGN KEY (bank_account_id) REFERENCES freelancer_bank_accounts(id);
