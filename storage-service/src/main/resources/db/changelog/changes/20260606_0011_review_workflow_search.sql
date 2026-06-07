--liquibase formatted sql

--changeset codex:20260606-0007-add-review-workflow-fields
--comment: Add neutral review workflow fields for evidence records without publishing classification judgments.
ALTER TABLE claims
    ADD COLUMN IF NOT EXISTS publish_status VARCHAR(50) NOT NULL DEFAULT 'INTERNAL_REVIEW'
        CHECK (publish_status IN ('INTERNAL_REVIEW', 'PUBLIC', 'NEEDS_REVIEW', 'REJECTED')),
    ADD COLUMN IF NOT EXISTS reviewed_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS review_notes TEXT;

ALTER TABLE public_statements
    ADD COLUMN IF NOT EXISTS publish_status VARCHAR(50) NOT NULL DEFAULT 'INTERNAL_REVIEW'
        CHECK (publish_status IN ('INTERNAL_REVIEW', 'PUBLIC', 'NEEDS_REVIEW', 'REJECTED')),
    ADD COLUMN IF NOT EXISTS reviewed_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS review_notes TEXT;

ALTER TABLE content_items
    ADD COLUMN IF NOT EXISTS publish_status VARCHAR(50) NOT NULL DEFAULT 'INTERNAL_REVIEW'
        CHECK (publish_status IN ('INTERNAL_REVIEW', 'PUBLIC', 'NEEDS_REVIEW', 'REJECTED')),
    ADD COLUMN IF NOT EXISTS reviewed_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS review_notes TEXT;
--rollback ALTER TABLE content_items DROP COLUMN IF EXISTS review_notes;
--rollback ALTER TABLE content_items DROP COLUMN IF EXISTS reviewed_at;
--rollback ALTER TABLE content_items DROP COLUMN IF EXISTS reviewed_by;
--rollback ALTER TABLE content_items DROP COLUMN IF EXISTS publish_status;
--rollback ALTER TABLE public_statements DROP COLUMN IF EXISTS review_notes;
--rollback ALTER TABLE public_statements DROP COLUMN IF EXISTS reviewed_at;
--rollback ALTER TABLE public_statements DROP COLUMN IF EXISTS reviewed_by;
--rollback ALTER TABLE public_statements DROP COLUMN IF EXISTS publish_status;
--rollback ALTER TABLE claims DROP COLUMN IF EXISTS review_notes;
--rollback ALTER TABLE claims DROP COLUMN IF EXISTS reviewed_by;
--rollback ALTER TABLE claims DROP COLUMN IF EXISTS publish_status;

--changeset codex:20260606-0008-enable-pg-trgm
--comment: Enable trigram indexes for fast local fuzzy text search.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
--rollback DROP EXTENSION IF EXISTS pg_trgm;

--changeset codex:20260606-0009-create-search-support-indexes
--comment: Speed up Postgres-backed demo search while Elasticsearch/OpenSearch reindexing is still maturing.
CREATE INDEX IF NOT EXISTS idx_bills_title_trgm_like ON bills USING gin (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_content_items_title_trgm_like ON content_items USING gin (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_public_statements_title_trgm_like ON public_statements USING gin (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_claims_text_trgm_like ON claims USING gin (claim_text gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_source_citations_title_trgm_like ON source_citations USING gin (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_claims_publish_status ON claims(publish_status);
CREATE INDEX IF NOT EXISTS idx_public_statements_publish_status ON public_statements(publish_status);
CREATE INDEX IF NOT EXISTS idx_content_items_publish_status ON content_items(publish_status);
--rollback DROP INDEX IF EXISTS idx_content_items_publish_status;
--rollback DROP INDEX IF EXISTS idx_public_statements_publish_status;
--rollback DROP INDEX IF EXISTS idx_claims_publish_status;
--rollback DROP INDEX IF EXISTS idx_source_citations_title_trgm_like;
--rollback DROP INDEX IF EXISTS idx_claims_text_trgm_like;
--rollback DROP INDEX IF EXISTS idx_public_statements_title_trgm_like;
--rollback DROP INDEX IF EXISTS idx_content_items_title_trgm_like;
--rollback DROP INDEX IF EXISTS idx_bills_title_trgm_like;
