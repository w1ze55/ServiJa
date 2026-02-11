-- V1__create_users_table.sql
-- Criação da tabela de usuários base

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(256) NOT NULL UNIQUE,
    cpf VARCHAR(14) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    password VARCHAR(256) NOT NULL,
    user_type VARCHAR(20) NOT NULL CHECK (user_type IN ('CONTRACTOR', 'FREELANCER')),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_code VARCHAR(6),
    verification_code_expiry TIMESTAMP NULL,
    reset_password_token VARCHAR(100),
    reset_password_token_expiry TIMESTAMP NULL,
    avatar_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índices para otimização
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_cpf ON users(cpf);
CREATE INDEX idx_users_user_type ON users(user_type);
CREATE INDEX idx_users_active ON users(active);

