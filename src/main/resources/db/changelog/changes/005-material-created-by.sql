--liquibase formatted sql

--changeset codex:005-material-created-by
ALTER TABLE materials
    RENAME COLUMN owner_id TO created_by;

DROP INDEX IF EXISTS idx_materials_owner_id;
CREATE INDEX IF NOT EXISTS idx_materials_created_by ON materials (created_by);
