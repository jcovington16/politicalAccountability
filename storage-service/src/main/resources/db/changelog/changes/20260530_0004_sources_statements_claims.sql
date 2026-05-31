--liquibase formatted sql

--changeset codex:20260530-0008-create-source-registry
--comment: Registry of official, primary, news, advocacy, social, and unknown sources.
CREATE TABLE IF NOT EXISTS source_registry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    source_type VARCHAR(50) NOT NULL CHECK (source_type IN ('OFFICIAL_RECORD', 'PRIMARY_SOURCE', 'REPUTABLE_NEWS', 'ADVOCACY_OR_PARTISAN', 'SOCIAL_MEDIA', 'UNKNOWN')),
    homepage_url TEXT,
    owning_entity VARCHAR(255),
    reputation_score NUMERIC(5, 2),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_source_registry_name_url UNIQUE (name, homepage_url)
);
--rollback DROP TABLE IF EXISTS source_registry CASCADE;

--changeset codex:20260530-0009-create-source-citations
--comment: Reusable citations for bills, votes, statements, claims, fact checks, news articles, and content items.
CREATE TABLE IF NOT EXISTS source_citations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id UUID REFERENCES source_registry(id) ON DELETE SET NULL,
    citation_type VARCHAR(50) NOT NULL CHECK (citation_type IN ('BILL', 'VOTE', 'STATEMENT', 'CLAIM', 'FACT_CHECK', 'NEWS_ARTICLE', 'CONTENT_ITEM', 'OFFICE', 'ELECTION')),
    target_id UUID,
    title TEXT,
    url TEXT NOT NULL,
    archive_url TEXT,
    published_at TIMESTAMP,
    retrieved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    quote TEXT,
    source_quality VARCHAR(50) NOT NULL CHECK (source_quality IN ('OFFICIAL_RECORD', 'PRIMARY_SOURCE', 'REPUTABLE_NEWS', 'ADVOCACY_OR_PARTISAN', 'SOCIAL_MEDIA', 'UNKNOWN')),
    confidence NUMERIC(5, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_source_citations_target_url UNIQUE (citation_type, target_id, url)
);
--rollback DROP TABLE IF EXISTS source_citations CASCADE;

--changeset codex:20260530-0010-create-public-statements
--comment: Public statements are modeled separately from generic content items for direct quote handling.
CREATE TABLE IF NOT EXISTS public_statements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    politician_id UUID REFERENCES politicians(id) ON DELETE CASCADE,
    statement_type VARCHAR(50) NOT NULL CHECK (statement_type IN ('SPEECH', 'INTERVIEW', 'SOCIAL', 'PRESS_RELEASE', 'DEBATE', 'HEARING', 'OTHER')),
    title TEXT NOT NULL,
    body TEXT,
    quote TEXT,
    venue VARCHAR(255),
    statement_date TIMESTAMP NOT NULL,
    source_citation_id UUID REFERENCES source_citations(id) ON DELETE SET NULL,
    confidence NUMERIC(5, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE IF EXISTS public_statements CASCADE;

--changeset codex:20260530-0011-create-claims
--comment: Claims separate verified facts, allegations, opinion, and unresolved claims.
CREATE TABLE IF NOT EXISTS claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    politician_id UUID REFERENCES politicians(id) ON DELETE SET NULL,
    statement_id UUID REFERENCES public_statements(id) ON DELETE SET NULL,
    claim_text TEXT NOT NULL,
    claim_type VARCHAR(50) NOT NULL CHECK (claim_type IN ('VERIFIED_FACT', 'DIRECT_QUOTE', 'VOTING_RECORD', 'ALLEGATION', 'OPINION_PIECE', 'UNRESOLVED_CLAIM')),
    status VARCHAR(50) NOT NULL DEFAULT 'UNRESOLVED' CHECK (status IN ('VERIFIED', 'DISPUTED', 'UNRESOLVED', 'RETRACTED')),
    confidence NUMERIC(5, 2),
    first_seen_at TIMESTAMP,
    last_reviewed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE IF EXISTS claims CASCADE;

--changeset codex:20260530-0012-create-fact-checks
--comment: Fact checks attach evidence and conclusions to claims.
CREATE TABLE IF NOT EXISTS fact_checks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES claims(id) ON DELETE CASCADE,
    rating VARCHAR(50) NOT NULL CHECK (rating IN ('TRUE', 'MOSTLY_TRUE', 'MIXED', 'MOSTLY_FALSE', 'FALSE', 'UNVERIFIED')),
    summary TEXT NOT NULL,
    checked_by VARCHAR(255),
    checked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source_citation_id UUID REFERENCES source_citations(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE IF EXISTS fact_checks CASCADE;

--changeset codex:20260530-0013-create-tags
--comment: Tags support issue and topic grouping across all record types.
CREATE TABLE IF NOT EXISTS tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL UNIQUE,
    category VARCHAR(80),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE IF EXISTS tags CASCADE;

--changeset codex:20260530-0014-create-taggings
CREATE TABLE IF NOT EXISTS taggings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tag_id UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    target_type VARCHAR(50) NOT NULL,
    target_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_taggings UNIQUE (tag_id, target_type, target_id)
);
--rollback DROP TABLE IF EXISTS taggings CASCADE;

--changeset codex:20260530-0015-create-source-statement-claim-indexes
CREATE INDEX IF NOT EXISTS idx_source_registry_type ON source_registry(source_type);
CREATE INDEX IF NOT EXISTS idx_source_citations_target ON source_citations(citation_type, target_id);
CREATE INDEX IF NOT EXISTS idx_source_citations_quality ON source_citations(source_quality);
CREATE INDEX IF NOT EXISTS idx_public_statements_politician_id ON public_statements(politician_id);
CREATE INDEX IF NOT EXISTS idx_public_statements_statement_date ON public_statements(statement_date);
CREATE INDEX IF NOT EXISTS idx_claims_politician_id ON claims(politician_id);
CREATE INDEX IF NOT EXISTS idx_claims_type_status ON claims(claim_type, status);
CREATE INDEX IF NOT EXISTS idx_fact_checks_claim_id ON fact_checks(claim_id);
CREATE INDEX IF NOT EXISTS idx_taggings_target ON taggings(target_type, target_id);
--rollback DROP INDEX IF EXISTS idx_taggings_target;
--rollback DROP INDEX IF EXISTS idx_fact_checks_claim_id;
--rollback DROP INDEX IF EXISTS idx_claims_type_status;
--rollback DROP INDEX IF EXISTS idx_claims_politician_id;
--rollback DROP INDEX IF EXISTS idx_public_statements_statement_date;
--rollback DROP INDEX IF EXISTS idx_public_statements_politician_id;
--rollback DROP INDEX IF EXISTS idx_source_citations_quality;
--rollback DROP INDEX IF EXISTS idx_source_citations_target;
--rollback DROP INDEX IF EXISTS idx_source_registry_type;
