-- Cria tabela de cidades de atendimento
CREATE TABLE service_cities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    freelancer_id BIGINT NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_service_cities_freelancer FOREIGN KEY (freelancer_id) REFERENCES freelancers(user_id) ON DELETE CASCADE,
    INDEX idx_freelancer_cities (freelancer_id),
    INDEX idx_city_state (city, state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
