CREATE TABLE favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    service_id BIGINT,
    freelancer_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_favorite_service FOREIGN KEY (service_id) REFERENCES services(id),
    CONSTRAINT fk_favorite_freelancer FOREIGN KEY (freelancer_id) REFERENCES freelancers(user_id),
    CONSTRAINT check_favorite_type CHECK (
        (service_id IS NOT NULL AND freelancer_id IS NULL) OR
        (service_id IS NULL AND freelancer_id IS NOT NULL)
    ),
    CONSTRAINT unique_user_service_favorite UNIQUE (user_id, service_id),
    CONSTRAINT unique_user_freelancer_favorite UNIQUE (user_id, freelancer_id)
);
