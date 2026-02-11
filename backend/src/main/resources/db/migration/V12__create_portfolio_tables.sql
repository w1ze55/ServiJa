-- Cria tabela de projetos do portfólio
CREATE TABLE portfolio_projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    freelancer_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    tags TEXT,
    video_url VARCHAR(500),
    project_url VARCHAR(500),
    completed_date DATE,
    client VARCHAR(200),
    featured BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_portfolio_projects_freelancer FOREIGN KEY (freelancer_id) REFERENCES freelancers(user_id) ON DELETE CASCADE,
    INDEX idx_freelancer_projects (freelancer_id),
    INDEX idx_featured (featured),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Cria tabela de imagens do portfólio
CREATE TABLE portfolio_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    image_url MEDIUMTEXT NOT NULL,
    caption VARCHAR(255),
    display_order INT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_portfolio_images_project FOREIGN KEY (project_id) REFERENCES portfolio_projects(id) ON DELETE CASCADE,
    INDEX idx_project_images (project_id),
    INDEX idx_display_order (project_id, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
