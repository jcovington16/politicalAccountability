--liquibase formatted sql

--changeset codex:20260604-0008-create-bill-sponsors
--comment: Track primary sponsors and cosponsors without overloading the legacy bills.introduced_by column.
CREATE TABLE IF NOT EXISTS bill_sponsors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bill_id UUID NOT NULL REFERENCES bills(id) ON DELETE CASCADE,
    politician_id UUID NOT NULL REFERENCES politicians(id) ON DELETE CASCADE,
    sponsor_type VARCHAR(50) NOT NULL CHECK (sponsor_type IN ('SPONSOR', 'COSPONSOR')),
    sponsorship_date DATE,
    source_citation_id UUID REFERENCES source_citations(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_bill_sponsors UNIQUE (bill_id, politician_id, sponsor_type)
);
--rollback DROP TABLE IF EXISTS bill_sponsors CASCADE;

--changeset codex:20260604-0009-create-bill-sponsor-indexes
CREATE INDEX IF NOT EXISTS idx_bill_sponsors_bill_id ON bill_sponsors(bill_id);
CREATE INDEX IF NOT EXISTS idx_bill_sponsors_politician_id ON bill_sponsors(politician_id);
CREATE INDEX IF NOT EXISTS idx_bill_sponsors_type ON bill_sponsors(sponsor_type);
--rollback DROP INDEX IF EXISTS idx_bill_sponsors_type;
--rollback DROP INDEX IF EXISTS idx_bill_sponsors_politician_id;
--rollback DROP INDEX IF EXISTS idx_bill_sponsors_bill_id;

--changeset codex:20260604-0010-create-public-statements-search-index
CREATE INDEX IF NOT EXISTS idx_public_statements_search
    ON public_statements
    USING gin (to_tsvector('english', coalesce(title, '') || ' ' || coalesce(body, '') || ' ' || coalesce(quote, '') || ' ' || coalesce(venue, '')));
--rollback DROP INDEX IF EXISTS idx_public_statements_search;
