--liquibase formatted sql

--changeset codex:004-users-and-roles
CREATE TABLE users (
    id uuid NOT NULL,
    email varchar(320) NULL,
    name varchar(512) NULL,
    picture_url varchar(2048) NULL,
    blocked_until timestamp with time zone NULL,
    blocked_reason varchar(2000) NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE TABLE user_roles (
    user_id uuid NOT NULL,
    role varchar(32) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_user_roles_role ON user_roles (role);

