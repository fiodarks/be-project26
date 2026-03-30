--liquibase formatted sql

--changeset codex:002-add-optimistic-locking
ALTER TABLE hierarchy_nodes
    ADD COLUMN version bigint NOT NULL DEFAULT 0;

ALTER TABLE materials
    ADD COLUMN version bigint NOT NULL DEFAULT 0;

