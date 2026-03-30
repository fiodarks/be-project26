--liquibase formatted sql

--changeset codex:007-user-password-hash
ALTER TABLE users
    ADD COLUMN password_hash varchar(255) NULL;

--changeset codex:007-users-email-unique
ALTER TABLE users
    ADD CONSTRAINT uq_users_email UNIQUE (email);
