ALTER TABLE addresses
    ADD COLUMN latitude DECIMAL(10,8) NULL AFTER zip_code,
    ADD COLUMN longitude DECIMAL(11,8) NULL AFTER latitude;
