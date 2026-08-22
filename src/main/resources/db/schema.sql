-- Global Recon Service schema
-- Run against an empty database when spring.jpa.hibernate.ddl-auto=none
-- Local H2: jdbc:h2:file:./data/global_recon  (user sa, empty password)
-- Compatible with H2 in PostgreSQL mode.

CREATE TABLE IF NOT EXISTS dataset (
    id                  VARCHAR(64)  PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    original_filename   VARCHAR(255) NOT NULL,
    format              VARCHAR(16)  NOT NULL,
    status              VARCHAR(32)  NOT NULL,
    row_count           BIGINT,
    error_message       VARCHAR(1024),
    owner_email         VARCHAR(320) NOT NULL,
    ingestion_notes     VARCHAR(4000),
    record_path         VARCHAR(255),
    created_at          TIMESTAMP    NOT NULL,
    updated_at          TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_dataset_owner ON dataset (owner_email);

CREATE TABLE IF NOT EXISTS dataset_record (
    id          VARCHAR(64) PRIMARY KEY,
    dataset_id  VARCHAR(64) NOT NULL,
    row_index   BIGINT      NOT NULL,
    payload     CLOB        NOT NULL,
    CONSTRAINT fk_dataset_record_dataset
        FOREIGN KEY (dataset_id) REFERENCES dataset (id)
);

CREATE INDEX IF NOT EXISTS idx_dataset_record_dataset ON dataset_record (dataset_id);

CREATE TABLE IF NOT EXISTS dataset_column (
    id               VARCHAR(64) PRIMARY KEY,
    dataset_id       VARCHAR(64) NOT NULL,
    column_name      VARCHAR(255) NOT NULL,
    detected_type    VARCHAR(16)  NOT NULL,
    null_count       BIGINT,
    null_percentage  DOUBLE PRECISION,
    distinct_count   BIGINT,
    unique_ratio     DOUBLE PRECISION,
    sample_values    CLOB,
    min_value        VARCHAR(255),
    max_value        VARCHAR(255),
    common_patterns  CLOB,
    ordinal_position INTEGER,
    CONSTRAINT fk_dataset_column_dataset
        FOREIGN KEY (dataset_id) REFERENCES dataset (id)
);

CREATE TABLE IF NOT EXISTS recon_plan (
    id                  VARCHAR(64)  PRIMARY KEY,
    left_dataset_id     VARCHAR(64)  NOT NULL,
    right_dataset_id    VARCHAR(64)  NOT NULL,
    owner_email         VARCHAR(320) NOT NULL,
    user_notes          CLOB,
    status              VARCHAR(32)  NOT NULL,
    overall_confidence  DOUBLE PRECISION,
    warnings            CLOB,
    created_at          TIMESTAMP    NOT NULL,
    approved_at         TIMESTAMP,
    CONSTRAINT fk_recon_plan_left_dataset
        FOREIGN KEY (left_dataset_id) REFERENCES dataset (id),
    CONSTRAINT fk_recon_plan_right_dataset
        FOREIGN KEY (right_dataset_id) REFERENCES dataset (id)
);

CREATE TABLE IF NOT EXISTS recon_key_mapping (
    id          VARCHAR(64)  PRIMARY KEY,
    plan_id     VARCHAR(64)  NOT NULL,
    left_field  VARCHAR(255) NOT NULL,
    right_field VARCHAR(255) NOT NULL,
    confidence  DOUBLE PRECISION,
    sort_order  INTEGER,
    CONSTRAINT fk_recon_key_mapping_plan
        FOREIGN KEY (plan_id) REFERENCES recon_plan (id)
);

CREATE TABLE IF NOT EXISTS recon_field_mapping (
    id          VARCHAR(64)  PRIMARY KEY,
    plan_id     VARCHAR(64)  NOT NULL,
    left_field  VARCHAR(255) NOT NULL,
    right_field VARCHAR(255) NOT NULL,
    match_type  VARCHAR(32)  NOT NULL,
    tolerance   DECIMAL(19, 8),
    confidence  DOUBLE PRECISION,
    sort_order  INTEGER,
    included    BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_recon_field_mapping_plan
        FOREIGN KEY (plan_id) REFERENCES recon_plan (id)
);

CREATE TABLE IF NOT EXISTS recon_job (
    id               VARCHAR(64)  PRIMARY KEY,
    type             VARCHAR(32)  NOT NULL,
    status           VARCHAR(32)  NOT NULL,
    left_dataset_id  VARCHAR(64),
    right_dataset_id VARCHAR(64),
    recon_plan_id    VARCHAR(64),
    result_id        VARCHAR(64),
    owner_email      VARCHAR(320) NOT NULL,
    notes            VARCHAR(4000),
    error_message    VARCHAR(1024),
    created_at       TIMESTAMP    NOT NULL,
    started_at       TIMESTAMP,
    completed_at     TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recon_job_status ON recon_job (status);
CREATE INDEX IF NOT EXISTS idx_recon_job_owner ON recon_job (owner_email);

CREATE TABLE IF NOT EXISTS recon_run (
    id                    VARCHAR(64)  PRIMARY KEY,
    recon_plan_id         VARCHAR(64)  NOT NULL,
    owner_email           VARCHAR(320) NOT NULL,
    status                VARCHAR(32)  NOT NULL,
    matched_count         BIGINT,
    break_count           BIGINT,
    only_in_left_count    BIGINT,
    only_in_right_count   BIGINT,
    duplicate_left_count  BIGINT,
    duplicate_right_count BIGINT,
    ambiguous_count       BIGINT,
    processed_count       BIGINT,
    name                  VARCHAR(255),
    saved_at              TIMESTAMP,
    error_message         VARCHAR(1024),
    started_at            TIMESTAMP    NOT NULL,
    completed_at          TIMESTAMP,
    CONSTRAINT fk_recon_run_plan
        FOREIGN KEY (recon_plan_id) REFERENCES recon_plan (id)
);

CREATE INDEX IF NOT EXISTS idx_recon_run_owner ON recon_run (owner_email);

CREATE TABLE IF NOT EXISTS recon_result (
    id               VARCHAR(64) PRIMARY KEY,
    run_id           VARCHAR(64) NOT NULL,
    status           VARCHAR(32) NOT NULL,
    left_record_id   VARCHAR(64),
    right_record_id  VARCHAR(64),
    recon_key        VARCHAR(255),
    differences      CLOB,
    CONSTRAINT fk_recon_result_run
        FOREIGN KEY (run_id) REFERENCES recon_run (id)
);

CREATE INDEX IF NOT EXISTS idx_recon_result_run ON recon_result (run_id);
CREATE INDEX IF NOT EXISTS idx_recon_result_run_status ON recon_result (run_id, status);
