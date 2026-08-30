CREATE TABLE IF NOT EXISTS company (
    id BIGINT NOT NULL AUTO_INCREMENT,
    address VARCHAR(600),
    country VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    data_source VARCHAR(255),
    legal_form VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    registration_number VARCHAR(255) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    watchlisted BIT(1) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_company_registration_number UNIQUE (registration_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_company_normalized_name ON company (normalized_name);
CREATE INDEX IF NOT EXISTS idx_company_registration_number ON company (registration_number);
CREATE INDEX IF NOT EXISTS idx_company_watchlisted ON company (watchlisted);

CREATE TABLE IF NOT EXISTS person (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_person_normalized_name ON person (normalized_name);

CREATE TABLE IF NOT EXISTS import_run (
    id BIGINT NOT NULL AUTO_INCREMENT,
    failed_rows INTEGER NOT NULL,
    finished_at DATETIME(6),
    imported_rows INTEGER NOT NULL,
    source_type VARCHAR(255) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    status VARCHAR(255) NOT NULL,
    total_rows INTEGER NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_import_run_started ON import_run (started_at);
CREATE INDEX IF NOT EXISTS idx_import_run_status ON import_run (status);

CREATE TABLE IF NOT EXISTS company_person_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    role VARCHAR(255) NOT NULL,
    company_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_company_person_role_company
        FOREIGN KEY (company_id) REFERENCES company (id),
    CONSTRAINT fk_company_person_role_person
        FOREIGN KEY (person_id) REFERENCES person (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_company_person_role_role ON company_person_role (role);
CREATE INDEX IF NOT EXISTS idx_company_person_role_company ON company_person_role (company_id);
CREATE INDEX IF NOT EXISTS idx_company_person_role_person ON company_person_role (person_id);

CREATE TABLE IF NOT EXISTS import_row_error (
    id BIGINT NOT NULL AUTO_INCREMENT,
    message VARCHAR(800) NOT NULL,
    raw_value VARCHAR(1200),
    source_row_number INTEGER NOT NULL,
    import_run_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_import_row_error_run
        FOREIGN KEY (import_run_id) REFERENCES import_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_import_row_error_run ON import_row_error (import_run_id);

CREATE TABLE IF NOT EXISTS change_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    archived BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    description VARCHAR(1200) NOT NULL,
    type VARCHAR(255) NOT NULL,
    company_id BIGINT,
    import_run_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_change_event_company
        FOREIGN KEY (company_id) REFERENCES company (id),
    CONSTRAINT fk_change_event_import_run
        FOREIGN KEY (import_run_id) REFERENCES import_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_change_company_created ON change_event (company_id, created_at);
CREATE INDEX IF NOT EXISTS idx_change_import_run_created ON change_event (import_run_id, created_at);
CREATE INDEX IF NOT EXISTS idx_change_type_created ON change_event (type, created_at);
CREATE INDEX IF NOT EXISTS idx_change_archived_created ON change_event (archived, created_at);

CREATE TABLE IF NOT EXISTS task_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    archived BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    done BIT(1) NOT NULL,
    priority VARCHAR(255) NOT NULL,
    segment VARCHAR(255) NOT NULL,
    title VARCHAR(240) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_task_done ON task_item (done);
CREATE INDEX IF NOT EXISTS idx_task_archived ON task_item (archived);
CREATE INDEX IF NOT EXISTS idx_task_segment ON task_item (segment);
