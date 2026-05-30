--liquibase formatted sql

--changeset codex:20260528-0001-enable-pgcrypto
--comment: Enable gen_random_uuid() for local PostgreSQL databases.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
--rollback DROP EXTENSION IF EXISTS pgcrypto;

--changeset codex:20260528-0002-create-politicians
CREATE TABLE IF NOT EXISTS politicians (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    party VARCHAR(100),
    state VARCHAR(50),
    office VARCHAR(100),
    biography TEXT,
    profile_image_url TEXT,
    start_date DATE NOT NULL,
    end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE IF EXISTS politicians CASCADE;

--changeset codex:20260528-0003-create-bills
CREATE TABLE IF NOT EXISTS bills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bill_number VARCHAR(50) UNIQUE NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    introduced_by UUID REFERENCES politicians(id),
    status VARCHAR(50) CHECK (status IN ('Pending', 'Passed', 'Failed', 'Vetoed')),
    introduced_date DATE NOT NULL,
    last_action_date DATE,
    bill_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE IF EXISTS bills CASCADE;

--changeset codex:20260528-0004-create-voting-records
CREATE TABLE IF NOT EXISTS voting_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    politician_id UUID REFERENCES politicians(id) ON DELETE CASCADE,
    bill_id UUID REFERENCES bills(id),
    vote_type VARCHAR(20) CHECK (vote_type IN ('YEA', 'NAY', 'ABSTAIN')),
    vote_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE IF EXISTS voting_records CASCADE;

--changeset codex:20260528-0005-create-media-files
CREATE TABLE IF NOT EXISTS media_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    politician_id UUID REFERENCES politicians(id) ON DELETE SET NULL,
    file_type VARCHAR(10) CHECK (file_type IN ('video', 'audio')),
    title TEXT,
    source TEXT,
    published_date TIMESTAMP,
    storage_url TEXT NOT NULL,
    transcript TEXT,
    tags TEXT[],
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE IF EXISTS media_files CASCADE;

--changeset codex:20260528-0006-create-content-items
CREATE TABLE IF NOT EXISTS content_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    text_body TEXT,
    media_url TEXT,
    published_at TIMESTAMP NOT NULL,
    content_hash VARCHAR(256) UNIQUE,
    source_url TEXT NOT NULL,
    politician_id UUID NOT NULL REFERENCES politicians(id) ON DELETE CASCADE,
    keywords TEXT[],
    tags TEXT[],
    indexed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE IF EXISTS content_items CASCADE;

--changeset codex:20260528-0007-create-provenance
CREATE TABLE IF NOT EXISTS provenance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_item_id UUID NOT NULL REFERENCES content_items(id) ON DELETE CASCADE,
    source_type VARCHAR(100),
    extractor_version VARCHAR(50),
    confidence FLOAT,
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE IF EXISTS provenance CASCADE;

--changeset codex:20260528-0008-create-indexes
CREATE INDEX IF NOT EXISTS idx_politicians_name ON politicians(last_name, first_name);
CREATE INDEX IF NOT EXISTS idx_politicians_state ON politicians(state);
CREATE INDEX IF NOT EXISTS idx_politicians_party ON politicians(party);
CREATE INDEX IF NOT EXISTS idx_content_items_politician_id ON content_items(politician_id);
CREATE INDEX IF NOT EXISTS idx_content_items_published_at ON content_items(published_at);
CREATE INDEX IF NOT EXISTS idx_content_items_content_hash ON content_items(content_hash);
CREATE INDEX IF NOT EXISTS idx_content_items_content_type ON content_items(content_type);
CREATE INDEX IF NOT EXISTS idx_provenance_source_type ON provenance(source_type);
--rollback DROP INDEX IF EXISTS idx_provenance_source_type;
--rollback DROP INDEX IF EXISTS idx_content_items_content_type;
--rollback DROP INDEX IF EXISTS idx_content_items_content_hash;
--rollback DROP INDEX IF EXISTS idx_content_items_published_at;
--rollback DROP INDEX IF EXISTS idx_content_items_politician_id;
--rollback DROP INDEX IF EXISTS idx_politicians_party;
--rollback DROP INDEX IF EXISTS idx_politicians_state;
--rollback DROP INDEX IF EXISTS idx_politicians_name;
