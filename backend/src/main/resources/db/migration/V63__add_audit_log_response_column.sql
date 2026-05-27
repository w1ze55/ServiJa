ALTER TABLE audit_logs
    ADD COLUMN response TEXT NULL AFTER payload;
