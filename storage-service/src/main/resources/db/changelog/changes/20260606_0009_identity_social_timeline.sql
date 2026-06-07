--liquibase formatted sql

--changeset codex:20260606-0001-expand-external-identifier-entity-types splitStatements:false
--comment: Let the identity graph map source IDs for social accounts, posts, statements, claims, and content records.
DO $$
DECLARE
    constraint_name text;
BEGIN
    SELECT conname INTO constraint_name
    FROM pg_constraint
    WHERE conrelid = 'external_identifiers'::regclass
      AND contype = 'c'
      AND pg_get_constraintdef(oid) LIKE '%entity_type%';

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE external_identifiers DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

ALTER TABLE external_identifiers
    ADD CONSTRAINT chk_external_identifiers_entity_type
    CHECK (entity_type IN (
        'POLITICIAN',
        'BILL',
        'VOTE',
        'OFFICE',
        'ELECTION',
        'CONTENT_ITEM',
        'STATEMENT',
        'CLAIM',
        'SOURCE',
        'SOCIAL_ACCOUNT',
        'SOCIAL_POST'
    ));
--rollback ALTER TABLE external_identifiers DROP CONSTRAINT IF EXISTS chk_external_identifiers_entity_type;

--changeset codex:20260606-0002-expand-source-citation-types splitStatements:false
--comment: Social accounts and social posts are cited separately from statements so account provenance stays visible.
DO $$
DECLARE
    constraint_name text;
BEGIN
    SELECT conname INTO constraint_name
    FROM pg_constraint
    WHERE conrelid = 'source_citations'::regclass
      AND contype = 'c'
      AND pg_get_constraintdef(oid) LIKE '%citation_type%';

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE source_citations DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

ALTER TABLE source_citations
    ADD CONSTRAINT chk_source_citations_citation_type
    CHECK (citation_type IN (
        'BILL',
        'VOTE',
        'STATEMENT',
        'CLAIM',
        'FACT_CHECK',
        'NEWS_ARTICLE',
        'CONTENT_ITEM',
        'OFFICE',
        'ELECTION',
        'SOCIAL_ACCOUNT',
        'SOCIAL_POST'
    ));
--rollback ALTER TABLE source_citations DROP CONSTRAINT IF EXISTS chk_source_citations_citation_type;

--changeset codex:20260606-0003-create-social-accounts
--comment: Verified/official social accounts are source records for public social posts.
CREATE TABLE IF NOT EXISTS social_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    politician_id UUID NOT NULL REFERENCES politicians(id) ON DELETE CASCADE,
    platform VARCHAR(50) NOT NULL CHECK (platform IN ('X', 'FACEBOOK', 'INSTAGRAM', 'THREADS', 'YOUTUBE', 'BLUESKY', 'MASTODON', 'TIKTOK', 'OTHER')),
    handle VARCHAR(255) NOT NULL,
    account_url TEXT NOT NULL,
    display_name VARCHAR(255),
    verification_status VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN' CHECK (verification_status IN ('OFFICIAL', 'VERIFIED', 'SELF_ASSERTED', 'UNKNOWN', 'DISPUTED')),
    source_citation_id UUID REFERENCES source_citations(id) ON DELETE SET NULL,
    confidence NUMERIC(5, 2) NOT NULL DEFAULT 0,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_social_accounts_platform_handle UNIQUE (platform, handle),
    CONSTRAINT chk_social_accounts_confidence CHECK (confidence >= 0 AND confidence <= 100)
);
--rollback DROP TABLE IF EXISTS social_accounts CASCADE;

--changeset codex:20260606-0004-create-social-account-indexes
CREATE INDEX IF NOT EXISTS idx_social_accounts_politician_id ON social_accounts(politician_id);
CREATE INDEX IF NOT EXISTS idx_social_accounts_platform ON social_accounts(platform);
CREATE INDEX IF NOT EXISTS idx_social_accounts_verification ON social_accounts(verification_status);
--rollback DROP INDEX IF EXISTS idx_social_accounts_verification;
--rollback DROP INDEX IF EXISTS idx_social_accounts_platform;
--rollback DROP INDEX IF EXISTS idx_social_accounts_politician_id;
