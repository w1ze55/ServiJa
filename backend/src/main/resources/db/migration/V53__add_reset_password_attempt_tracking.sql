ALTER TABLE users
    ADD COLUMN reset_password_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN reset_password_lock_until DATETIME NULL;
