CREATE TABLE review_ai_summaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    freelancer_id BIGINT NOT NULL UNIQUE,
    summary_json TEXT NOT NULL,
    review_count INT NOT NULL DEFAULT 0,
    generated_at DATETIME NOT NULL,
    CONSTRAINT fk_ras_freelancer FOREIGN KEY (freelancer_id) REFERENCES freelancers(user_id) ON DELETE CASCADE
);
