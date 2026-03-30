--liquibase formatted sql

--changeset codex:003-hierarchy-node-status
ALTER TABLE hierarchy_nodes
    ADD COLUMN status varchar(20) NOT NULL DEFAULT 'APPROVED';

CREATE INDEX idx_hierarchy_nodes_parent_id ON hierarchy_nodes (parent_id);
CREATE UNIQUE INDEX uk_hierarchy_nodes_parent_level_name ON hierarchy_nodes (parent_id, level, name);
