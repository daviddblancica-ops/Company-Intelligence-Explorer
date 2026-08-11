ALTER TABLE change_event
    ADD COLUMN IF NOT EXISTS severity VARCHAR(20) NULL AFTER type;

UPDATE change_event
SET severity = CASE
    WHEN type LIKE '%FAILED%' OR type LIKE '%ERROR%' THEN 'CRITICAL'
    WHEN type LIKE '%WATCHLIST%' OR type LIKE '%PERSON%' OR type LIKE '%PARTIAL%' THEN 'WARNING'
    ELSE 'INFO'
END
WHERE severity IS NULL OR severity = '';

ALTER TABLE change_event
    MODIFY COLUMN severity VARCHAR(20) NOT NULL;

CREATE INDEX IF NOT EXISTS idx_change_severity_created
    ON change_event (severity, created_at);
