CREATE TABLE support_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    requester_id BIGINT NOT NULL,
    admin_user_id BIGINT NULL,
    category ENUM(
        'BUG',
        'CATEGORY_SUBCATEGORY_REQUEST',
        'PAYMENT_ISSUE',
        'SERVICE_ISSUE',
        'FRAUD',
        'USER_REPORT'
    ) NOT NULL,
    description TEXT NOT NULL,
    attachments LONGTEXT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'ANSWERED') NOT NULL DEFAULT 'PENDING',
    admin_response TEXT NULL,
    answered_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_support_requests_requester_id (requester_id),
    KEY idx_support_requests_admin_user_id (admin_user_id),
    KEY idx_support_requests_status (status),
    KEY idx_support_requests_category (category),
    CONSTRAINT fk_support_requests_requester
        FOREIGN KEY (requester_id) REFERENCES users (id),
    CONSTRAINT fk_support_requests_admin_user
        FOREIGN KEY (admin_user_id) REFERENCES users (id)
);
