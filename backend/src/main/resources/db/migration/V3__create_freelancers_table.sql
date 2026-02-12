-- V3__create_freelancers_table.sql
-- Criação da tabela de freelancers (herança de users)

CREATE TABLE freelancers (
    user_id BIGINT PRIMARY KEY,
    bio TEXT,
    professional_title VARCHAR(255),
    location VARCHAR(255),
    rating DECIMAL(3, 2),
    total_reviews INT NOT NULL DEFAULT 0,
    completed_jobs INT NOT NULL DEFAULT 0,
    active_jobs INT NOT NULL DEFAULT 0,
    kyc_completed BOOLEAN NOT NULL DEFAULT FALSE,
    profile_completed BOOLEAN NOT NULL DEFAULT FALSE,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_freelancers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_rating_range CHECK (rating IS NULL OR (rating >= 0 AND rating <= 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índices para otimização
CREATE INDEX idx_freelancers_location ON freelancers(location);
CREATE INDEX idx_freelancers_rating ON freelancers(rating);
CREATE INDEX idx_freelancers_available ON freelancers(available);
CREATE INDEX idx_freelancers_kyc_completed ON freelancers(kyc_completed);

