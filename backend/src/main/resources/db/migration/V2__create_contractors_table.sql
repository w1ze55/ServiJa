-- V2__create_contractors_table.sql
-- Criação da tabela de contratantes (herança de users)

CREATE TABLE contractors (
    user_id BIGINT PRIMARY KEY,
    preferred_location VARCHAR(255),
    total_contracts INT NOT NULL DEFAULT 0,
    active_contracts INT NOT NULL DEFAULT 0,
    completed_contracts INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_contractors_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índices para otimização
CREATE INDEX idx_contractors_location ON contractors(preferred_location);

