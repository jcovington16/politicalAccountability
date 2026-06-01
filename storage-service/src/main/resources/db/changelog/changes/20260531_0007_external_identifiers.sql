--liquibase formatted sql

--changeset codex:20260531-0024-create-external-identifiers
--comment: Map official-source identifiers to internal records for dedupe and repeat imports.
CREATE TABLE IF NOT EXISTS external_identifiers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL CHECK (entity_type IN ('POLITICIAN', 'BILL', 'VOTE', 'OFFICE', 'ELECTION')),
    entity_id UUID NOT NULL,
    source_system VARCHAR(100) NOT NULL,
    external_id TEXT NOT NULL,
    source_url TEXT,
    confidence NUMERIC(5, 2),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_external_identifiers_source UNIQUE (entity_type, source_system, external_id)
);
--rollback DROP TABLE IF EXISTS external_identifiers CASCADE;

--changeset codex:20260531-0025-create-external-identifier-indexes
CREATE INDEX IF NOT EXISTS idx_external_identifiers_entity ON external_identifiers(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_external_identifiers_source_system ON external_identifiers(source_system);
--rollback DROP INDEX IF EXISTS idx_external_identifiers_source_system;
--rollback DROP INDEX IF EXISTS idx_external_identifiers_entity;
