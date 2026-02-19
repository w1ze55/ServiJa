ALTER TABLE users ADD COLUMN verification_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN verification_lock_until DATETIME NULL;
