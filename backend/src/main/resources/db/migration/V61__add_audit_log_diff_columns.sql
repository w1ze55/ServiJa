ALTER TABLE audit_logs
    ADD COLUMN old_value TEXT NULL AFTER payload,
    ADD COLUMN new_value TEXT NULL AFTER old_value;
