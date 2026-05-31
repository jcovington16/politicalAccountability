--liquibase formatted sql

--changeset codex:20260530-0016-create-import-batches
--comment: Track each file/API ingestion run for auditability and retry management.
CREATE TABLE IF NOT EXISTS import_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_system VARCHAR(100) NOT NULL,
    source_detail TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'STARTED' CHECK (status IN ('STARTED', 'COMPLETED', 'FAILED')),
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    records_seen INTEGER NOT NULL DEFAULT 0,
    records_imported INTEGER NOT NULL DEFAULT 0,
    records_skipped INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);
--rollback DROP TABLE IF EXISTS import_batches CASCADE;

--changeset codex:20260530-0017-create-import-row-results
CREATE TABLE IF NOT EXISTS import_row_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    import_batch_id UUID NOT NULL REFERENCES import_batches(id) ON DELETE CASCADE,
    source_record_id TEXT,
    target_type VARCHAR(80),
    target_id UUID,
    status VARCHAR(50) NOT NULL CHECK (status IN ('IMPORTED', 'SKIPPED', 'FAILED')),
    message TEXT,
    row_payload JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE IF EXISTS import_row_results CASCADE;

--changeset codex:20260530-0018-create-bill-actions
CREATE TABLE IF NOT EXISTS bill_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bill_id UUID NOT NULL REFERENCES bills(id) ON DELETE CASCADE,
    action_date DATE NOT NULL,
    action_text TEXT NOT NULL,
    source_citation_id UUID REFERENCES source_citations(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_bill_actions UNIQUE (bill_id, action_date, action_text)
);
--rollback DROP TABLE IF EXISTS bill_actions CASCADE;

--changeset codex:20260530-0019-create-import-action-indexes
CREATE INDEX IF NOT EXISTS idx_import_batches_source_status ON import_batches(source_system, status);
CREATE INDEX IF NOT EXISTS idx_import_row_results_batch ON import_row_results(import_batch_id);
CREATE INDEX IF NOT EXISTS idx_import_row_results_target ON import_row_results(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_bill_actions_bill_id ON bill_actions(bill_id);
CREATE INDEX IF NOT EXISTS idx_bill_actions_action_date ON bill_actions(action_date);
--rollback DROP INDEX IF EXISTS idx_bill_actions_action_date;
--rollback DROP INDEX IF EXISTS idx_bill_actions_bill_id;
--rollback DROP INDEX IF EXISTS idx_import_row_results_target;
--rollback DROP INDEX IF EXISTS idx_import_row_results_batch;
--rollback DROP INDEX IF EXISTS idx_import_batches_source_status;
