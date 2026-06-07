--liquibase formatted sql

--changeset codex:20260606-0005-create-civic-classifications
--comment: Store transparent classification/review labels for statements, claims, content, social posts, bills, and votes.
CREATE TABLE IF NOT EXISTS civic_classifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    target_type VARCHAR(50) NOT NULL CHECK (target_type IN ('STATEMENT', 'CLAIM', 'CONTENT_ITEM', 'SOCIAL_POST', 'BILL', 'VOTE', 'POLITICIAN_PROFILE')),
    target_id UUID NOT NULL,
    sentiment VARCHAR(50) NOT NULL CHECK (sentiment IN ('POSITIVE', 'NEGATIVE', 'MIXED', 'NEUTRAL', 'UNKNOWN')),
    civic_impact VARCHAR(50) NOT NULL CHECK (civic_impact IN ('PROBLEM_SOLVING', 'PROBLEMATIC', 'HARMFUL_RISK', 'PUBLIC_SERVICE', 'ACCOUNTABILITY_CONCERN', 'INFORMATIONAL', 'UNKNOWN')),
    harm_risk VARCHAR(50) NOT NULL CHECK (harm_risk IN ('NONE', 'LOW', 'MEDIUM', 'HIGH')),
    problem_solving BOOLEAN NOT NULL DEFAULT false,
    problematic BOOLEAN NOT NULL DEFAULT false,
    review_status VARCHAR(50) NOT NULL DEFAULT 'MACHINE_CLASSIFIED' CHECK (review_status IN ('MACHINE_CLASSIFIED', 'NEEDS_REVIEW', 'HUMAN_REVIEWED', 'DISPUTED', 'REJECTED')),
    confidence VARCHAR(50) NOT NULL CHECK (confidence IN ('HIGH', 'MEDIUM', 'LOW')),
    labels JSONB NOT NULL DEFAULT '[]'::jsonb,
    explanation TEXT NOT NULL,
    review_warnings JSONB NOT NULL DEFAULT '[]'::jsonb,
    classified_by VARCHAR(255) NOT NULL DEFAULT 'system',
    reviewed_by VARCHAR(255),
    reviewed_at TIMESTAMP,
    model_version VARCHAR(100) NOT NULL DEFAULT 'rule-based-v1',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_civic_classifications_target_version UNIQUE (target_type, target_id, model_version)
);
--rollback DROP TABLE IF EXISTS civic_classifications CASCADE;

--changeset codex:20260606-0006-create-civic-classification-indexes
CREATE INDEX IF NOT EXISTS idx_civic_classifications_target ON civic_classifications(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_civic_classifications_review_status ON civic_classifications(review_status);
CREATE INDEX IF NOT EXISTS idx_civic_classifications_impact ON civic_classifications(civic_impact);
CREATE INDEX IF NOT EXISTS idx_civic_classifications_harm_risk ON civic_classifications(harm_risk);
--rollback DROP INDEX IF EXISTS idx_civic_classifications_harm_risk;
--rollback DROP INDEX IF EXISTS idx_civic_classifications_impact;
--rollback DROP INDEX IF EXISTS idx_civic_classifications_review_status;
--rollback DROP INDEX IF EXISTS idx_civic_classifications_target;
