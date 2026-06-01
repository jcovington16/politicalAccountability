--liquibase formatted sql

--changeset codex:20260531-0020-add-import-batch-checksum
--comment: Store optional source checksums for file/API import reproducibility.
ALTER TABLE import_batches
    ADD COLUMN IF NOT EXISTS source_checksum TEXT;
--rollback ALTER TABLE import_batches DROP COLUMN IF EXISTS source_checksum;

--changeset codex:20260531-0021-create-audit-log
--comment: Append-only audit history for imports, source decisions, trust labels, and admin actions.
CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_type VARCHAR(50) NOT NULL CHECK (actor_type IN ('SYSTEM', 'IMPORT_OPERATOR', 'ADMIN', 'ANALYST', 'PUBLIC_USER')),
    actor_id UUID,
    action VARCHAR(120) NOT NULL,
    target_type VARCHAR(80) NOT NULL,
    target_id UUID,
    source_system VARCHAR(100),
    import_batch_id UUID REFERENCES import_batches(id) ON DELETE SET NULL,
    request_id TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE IF EXISTS audit_log CASCADE;

--changeset codex:20260531-0022-create-audit-log-indexes
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_target ON audit_log(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_actor ON audit_log(actor_type, actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_import_batch ON audit_log(import_batch_id);
--rollback DROP INDEX IF EXISTS idx_audit_log_import_batch;
--rollback DROP INDEX IF EXISTS idx_audit_log_actor;
--rollback DROP INDEX IF EXISTS idx_audit_log_target;
--rollback DROP INDEX IF EXISTS idx_audit_log_created_at;

--changeset codex:20260531-0023-prevent-audit-log-mutation splitStatements:false
CREATE OR REPLACE FUNCTION prevent_audit_log_mutation()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_log is append-only';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_audit_log_update ON audit_log;
CREATE TRIGGER trg_prevent_audit_log_update
    BEFORE UPDATE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_log_mutation();

DROP TRIGGER IF EXISTS trg_prevent_audit_log_delete ON audit_log;
CREATE TRIGGER trg_prevent_audit_log_delete
    BEFORE DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_log_mutation();
--rollback DROP TRIGGER IF EXISTS trg_prevent_audit_log_delete ON audit_log;
--rollback DROP TRIGGER IF EXISTS trg_prevent_audit_log_update ON audit_log;
--rollback DROP FUNCTION IF EXISTS prevent_audit_log_mutation();
