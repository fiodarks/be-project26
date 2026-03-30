--liquibase formatted sql

--changeset codex:001-initial-schema
CREATE TABLE hierarchy_nodes (
    id uuid NOT NULL,
    parent_id uuid NULL,
    level int NOT NULL,
    name varchar(255) NOT NULL,
    description varchar(1000) NULL,
    CONSTRAINT pk_hierarchy_nodes PRIMARY KEY (id),
    CONSTRAINT fk_hierarchy_nodes_parent FOREIGN KEY (parent_id) REFERENCES hierarchy_nodes (id) ON DELETE SET NULL
);

CREATE TABLE materials (
    id uuid NOT NULL,
    title varchar(255) NOT NULL,
    location varchar(255) NOT NULL,
    place_id varchar(255) NULL,
    lat double precision NULL,
    lon double precision NULL,
    creation_date_raw varchar(10) NOT NULL,
    creation_date_from date NOT NULL,
    creation_date_to date NOT NULL,
    description varchar(10000) NOT NULL,
    hierarchy_id uuid NOT NULL,
    owner_id uuid NOT NULL,
    file_url varchar(2048) NULL,
    thumbnail_url varchar(2048) NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    CONSTRAINT pk_materials PRIMARY KEY (id),
    CONSTRAINT fk_materials_hierarchy FOREIGN KEY (hierarchy_id) REFERENCES hierarchy_nodes (id) ON DELETE RESTRICT
);

CREATE TABLE material_metadata (
    material_id uuid NOT NULL,
    meta_key varchar(255) NOT NULL,
    meta_value varchar(2000) NULL,
    CONSTRAINT pk_material_metadata PRIMARY KEY (material_id, meta_key),
    CONSTRAINT fk_material_metadata_material FOREIGN KEY (material_id) REFERENCES materials (id) ON DELETE CASCADE
);

CREATE TABLE material_tags (
    material_id uuid NOT NULL,
    tag varchar(255) NOT NULL,
    CONSTRAINT pk_material_tags PRIMARY KEY (material_id, tag),
    CONSTRAINT fk_material_tags_material FOREIGN KEY (material_id) REFERENCES materials (id) ON DELETE CASCADE
);

CREATE INDEX idx_materials_created_at ON materials (created_at);
CREATE INDEX idx_materials_owner_id ON materials (owner_id);
CREATE INDEX idx_materials_place_id ON materials (place_id);
CREATE INDEX idx_materials_hierarchy_id ON materials (hierarchy_id);
CREATE INDEX idx_materials_creation_date_from ON materials (creation_date_from);
CREATE INDEX idx_materials_creation_date_to ON materials (creation_date_to);
