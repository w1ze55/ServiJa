-- Cria tabela de links profissionais
CREATE TABLE professional_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    freelancer_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    url VARCHAR(500) NOT NULL,
    label VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_professional_links_freelancer FOREIGN KEY (freelancer_id) REFERENCES freelancers(user_id) ON DELETE CASCADE,
    INDEX idx_freelancer_links (freelancer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
