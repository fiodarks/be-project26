--liquibase formatted sql

--changeset codex:008-audit-events
CREATE TABLE audit_events (
    id uuid NOT NULL,
    at timestamp with time zone NOT NULL,
    action varchar(64) NOT NULL,
    actor_user_id uuid NOT NULL,
    target_user_id uuid NULL,
    material_id uuid NULL,
    reason varchar(2000) NULL,
    details_json text NULL,
    CONSTRAINT pk_audit_events PRIMARY KEY (id)
);

CREATE INDEX idx_audit_events_at ON audit_events (at);
CREATE INDEX idx_audit_events_action ON audit_events (action);
CREATE INDEX idx_audit_events_actor_user_id ON audit_events (actor_user_id);
CREATE INDEX idx_audit_events_target_user_id ON audit_events (target_user_id);
CREATE INDEX idx_audit_events_material_id ON audit_events (material_id);
