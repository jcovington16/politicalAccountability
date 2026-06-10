--liquibase formatted sql

--changeset codex:20260608-0010-hot-path-query-indexes
--comment: Add read-path indexes for search, profile aggregation, bill detail, timeline, and citation lookups.
CREATE INDEX IF NOT EXISTS idx_voting_records_politician_date ON voting_records(politician_id, vote_date DESC);
CREATE INDEX IF NOT EXISTS idx_voting_records_bill_date ON voting_records(bill_id, vote_date DESC);
CREATE INDEX IF NOT EXISTS idx_bills_introduced_by_date ON bills(introduced_by, introduced_date DESC);
CREATE INDEX IF NOT EXISTS idx_bills_last_action_date ON bills(last_action_date DESC);
CREATE INDEX IF NOT EXISTS idx_bill_actions_bill_date ON bill_actions(bill_id, action_date DESC);
CREATE INDEX IF NOT EXISTS idx_source_citations_target_retrieved ON source_citations(citation_type, target_id, retrieved_at DESC);
CREATE INDEX IF NOT EXISTS idx_source_citations_quality_retrieved ON source_citations(source_quality, retrieved_at DESC);
CREATE INDEX IF NOT EXISTS idx_public_statements_politician_date ON public_statements(politician_id, statement_date DESC);
CREATE INDEX IF NOT EXISTS idx_claims_politician_status_type ON claims(politician_id, status, claim_type);
CREATE INDEX IF NOT EXISTS idx_content_items_politician_published ON content_items(politician_id, published_at DESC);
CREATE INDEX IF NOT EXISTS idx_import_batches_started ON import_batches(started_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_created ON audit_log(created_at DESC);
--rollback DROP INDEX IF EXISTS idx_audit_log_created;
--rollback DROP INDEX IF EXISTS idx_import_batches_started;
--rollback DROP INDEX IF EXISTS idx_content_items_politician_published;
--rollback DROP INDEX IF EXISTS idx_claims_politician_status_type;
--rollback DROP INDEX IF EXISTS idx_public_statements_politician_date;
--rollback DROP INDEX IF EXISTS idx_source_citations_quality_retrieved;
--rollback DROP INDEX IF EXISTS idx_source_citations_target_retrieved;
--rollback DROP INDEX IF EXISTS idx_bill_actions_bill_date;
--rollback DROP INDEX IF EXISTS idx_bills_last_action_date;
--rollback DROP INDEX IF EXISTS idx_bills_introduced_by_date;
--rollback DROP INDEX IF EXISTS idx_voting_records_bill_date;
--rollback DROP INDEX IF EXISTS idx_voting_records_politician_date;
