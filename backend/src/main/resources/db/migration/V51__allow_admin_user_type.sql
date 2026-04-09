ALTER TABLE users
    DROP CHECK users_chk_1;

ALTER TABLE users
    ADD CONSTRAINT chk_users_user_type
        CHECK (user_type IN ('CONTRACTOR', 'FREELANCER', 'ADMIN'));
