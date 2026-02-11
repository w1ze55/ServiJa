-- Criar tabela de serviços oferecidos pelos freelancers
CREATE TABLE services (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    freelancer_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    price_type ENUM('FIXED', 'HOURLY', 'DAILY') NOT NULL DEFAULT 'FIXED',
    delivery_time INT NOT NULL,
    delivery_unit ENUM('DAYS', 'WEEKS') NOT NULL DEFAULT 'DAYS',
    tags TEXT,
    images TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    FOREIGN KEY (freelancer_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Criar índices para buscas
CREATE INDEX idx_services_freelancer ON services (freelancer_id);
CREATE INDEX idx_services_category ON services (category);
CREATE INDEX idx_services_is_active ON services (is_active);
CREATE INDEX idx_services_created_at ON services (created_at);
