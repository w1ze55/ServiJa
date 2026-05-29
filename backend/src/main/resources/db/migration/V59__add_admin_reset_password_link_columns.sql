ALTER TABLE users
    ADD COLUMN admin_reset_password_token_hash VARCHAR(64) NULL,
    ADD COLUMN admin_reset_password_token_expiry DATETIME NULL;

CREATE INDEX idx_users_admin_reset_password_token_hash
    ON users (admin_reset_password_token_hash);
