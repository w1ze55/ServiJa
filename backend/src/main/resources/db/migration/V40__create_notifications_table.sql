CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    actor_id BIGINT NULL,
    type VARCHAR(50) NOT NULL,
    category VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    is_read BIT(1) NOT NULL DEFAULT b'0',
    read_at DATETIME NULL,
    metadata_json TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_recipient
        FOREIGN KEY (recipient_id) REFERENCES users(id),
    CONSTRAINT fk_notifications_actor
        FOREIGN KEY (actor_id) REFERENCES users(id),
    INDEX idx_notifications_recipient_created_at (recipient_id, created_at DESC),
    INDEX idx_notifications_recipient_is_read (recipient_id, is_read),
    INDEX idx_notifications_recipient_type (recipient_id, type)
);
