ALTER TABLE users
    ADD COLUMN active_role VARCHAR(20) NULL AFTER user_type;

UPDATE users
SET active_role = user_type
WHERE active_role IS NULL;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_user_roles_role CHECK (role IN ('CONTRACTOR', 'FREELANCER', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO user_roles (user_id, role)
SELECT id, user_type
FROM users
WHERE user_type IS NOT NULL;

INSERT IGNORE INTO user_roles (user_id, role)
SELECT user_id, 'CONTRACTOR'
FROM contractors;

INSERT IGNORE INTO user_roles (user_id, role)
SELECT user_id, 'FREELANCER'
FROM freelancers;

ALTER TABLE users
    MODIFY COLUMN active_role VARCHAR(20) NOT NULL;

CREATE INDEX idx_users_active_role ON users(active_role);
CREATE INDEX idx_user_roles_role ON user_roles(role);
