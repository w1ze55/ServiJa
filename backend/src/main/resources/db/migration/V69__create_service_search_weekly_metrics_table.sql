CREATE TABLE IF NOT EXISTS service_search_weekly_metrics (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    service_id BIGINT NOT NULL,
    week_start_date DATE NOT NULL,
    search_count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_search_weekly_metrics_service
        FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE CASCADE,
    CONSTRAINT uk_service_search_weekly_metrics_service_week
        UNIQUE (service_id, week_start_date),
    INDEX idx_service_search_weekly_metrics_week_count (week_start_date, search_count),
    INDEX idx_service_search_weekly_metrics_service (service_id)
);
