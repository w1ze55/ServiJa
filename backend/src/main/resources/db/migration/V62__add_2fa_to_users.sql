-- 2FA TOTP support (Google Authenticator)
ALTER TABLE users
    ADD COLUMN totp_secret_encrypted VARBINARY(255) NULL,
    ADD COLUMN pending_totp_secret_encrypted VARBINARY(255) NULL,
    ADD COLUMN pending_totp_created_at DATETIME NULL,
    ADD COLUMN totp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN totp_enabled_at DATETIME NULL,
    ADD COLUMN totp_failed_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN totp_locked_until DATETIME NULL;

CREATE TABLE user_2fa_backup_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_2fa_backup_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_2fa_backup_user (user_id)
);
