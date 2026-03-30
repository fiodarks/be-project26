--liquibase formatted sql

--changeset codex:006-user-surname
ALTER TABLE users
    ADD COLUMN surname varchar(512) NULL;

