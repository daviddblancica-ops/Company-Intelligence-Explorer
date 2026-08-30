ALTER TABLE change_event
    MODIFY COLUMN company_id BIGINT NULL;

ALTER TABLE change_event
    ADD COLUMN IF NOT EXISTS import_run_id BIGINT NULL;

CREATE INDEX IF NOT EXISTS idx_change_import_run_created
    ON change_event (import_run_id, created_at);
