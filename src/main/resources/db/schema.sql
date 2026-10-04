-- Global Recon Service schema for MySQL 8
-- Run on an empty database, then set spring.jpa.hibernate.ddl-auto=none
-- Example: mysql -u <user> -p <database> < src/main/resources/db/schema.sql
-- Microsoft SQL Server: use schema-sqlserver.sql instead.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS dataset (
    id                  VARCHAR(64)  NOT NULL,
    name                VARCHAR(255) NOT NULL,
    original_filename   VARCHAR(255) NOT NULL,
    format              VARCHAR(16)  NOT NULL,
    status              VARCHAR(32)  NOT NULL,
    row_count           BIGINT,
    error_message       VARCHAR(1024),
    owner_email         VARCHAR(320) NOT NULL,
    ingestion_notes     VARCHAR(4000),
    record_path         VARCHAR(255),
    source_kind         VARCHAR(16)  NOT NULL DEFAULT 'FILE',
    source_id           VARCHAR(64),
    source_params_json  JSON,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_dataset_owner (owner_email),
    KEY idx_dataset_source (source_id),
    KEY idx_dataset_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS dataset_record (
    id          VARCHAR(64) NOT NULL,
    dataset_id  VARCHAR(64) NOT NULL,
    row_index   BIGINT      NOT NULL,
    payload     LONGTEXT    NOT NULL,
    PRIMARY KEY (id),
    KEY idx_dataset_record_dataset (dataset_id),
    CONSTRAINT fk_dataset_record_dataset
        FOREIGN KEY (dataset_id) REFERENCES dataset (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS dataset_column (
    id               VARCHAR(64)  NOT NULL,
    dataset_id       VARCHAR(64)  NOT NULL,
    column_name      VARCHAR(255) NOT NULL,
    detected_type    VARCHAR(16)  NOT NULL,
    null_count       BIGINT,
    null_percentage  DOUBLE,
    distinct_count   BIGINT,
    unique_ratio     DOUBLE,
    sample_values    LONGTEXT,
    min_value        VARCHAR(255),
    max_value        VARCHAR(255),
    common_patterns  LONGTEXT,
    ordinal_position INT,
    PRIMARY KEY (id),
    CONSTRAINT fk_dataset_column_dataset
        FOREIGN KEY (dataset_id) REFERENCES dataset (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS recon_plan (
    id                  VARCHAR(64)  NOT NULL,
    left_dataset_id     VARCHAR(64)  NOT NULL,
    right_dataset_id    VARCHAR(64)  NOT NULL,
    owner_email         VARCHAR(320) NOT NULL,
    user_notes          LONGTEXT,
    status              VARCHAR(32)  NOT NULL,
    overall_confidence  DOUBLE,
    warnings            LONGTEXT,
    created_at          DATETIME(6)  NOT NULL,
    approved_at         DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_recon_plan_left_dataset
        FOREIGN KEY (left_dataset_id) REFERENCES dataset (id),
    CONSTRAINT fk_recon_plan_right_dataset
        FOREIGN KEY (right_dataset_id) REFERENCES dataset (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS recon_key_mapping (
    id          VARCHAR(64)  NOT NULL,
    plan_id     VARCHAR(64)  NOT NULL,
    left_field  VARCHAR(255) NOT NULL,
    right_field VARCHAR(255) NOT NULL,
    confidence  DOUBLE,
    sort_order  INT,
    PRIMARY KEY (id),
    CONSTRAINT fk_recon_key_mapping_plan
        FOREIGN KEY (plan_id) REFERENCES recon_plan (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS recon_field_mapping (
    id          VARCHAR(64)  NOT NULL,
    plan_id     VARCHAR(64)  NOT NULL,
    left_field  VARCHAR(255) NOT NULL,
    right_field VARCHAR(255) NOT NULL,
    match_type  VARCHAR(32)  NOT NULL,
    tolerance   DECIMAL(19, 8),
    confidence  DOUBLE,
    sort_order  INT,
    included    TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_recon_field_mapping_plan
        FOREIGN KEY (plan_id) REFERENCES recon_plan (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS recon_job (
    id               VARCHAR(64)  NOT NULL,
    type             VARCHAR(32)  NOT NULL,
    status           VARCHAR(32)  NOT NULL,
    left_dataset_id  VARCHAR(64),
    right_dataset_id VARCHAR(64),
    recon_plan_id    VARCHAR(64),
    result_id        VARCHAR(64),
    owner_email      VARCHAR(320) NOT NULL,
    notes            VARCHAR(4000),
    error_message    VARCHAR(1024),
    created_at       DATETIME(6)  NOT NULL,
    started_at       DATETIME(6),
    completed_at     DATETIME(6),
    collection_cycle_id VARCHAR(64),
    collection_item_id  VARCHAR(64),
    PRIMARY KEY (id),
    KEY idx_recon_job_status (status),
    KEY idx_recon_job_owner (owner_email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS recon_run (
    id                    VARCHAR(64)  NOT NULL,
    recon_plan_id         VARCHAR(64)  NOT NULL,
    left_dataset_id       VARCHAR(64),
    right_dataset_id      VARCHAR(64),
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
    saved_at              DATETIME(6),
    error_message         VARCHAR(1024),
    started_at            DATETIME(6)  NOT NULL,
    completed_at          DATETIME(6),
    PRIMARY KEY (id),
    KEY idx_recon_run_owner (owner_email),
    CONSTRAINT fk_recon_run_plan
        FOREIGN KEY (recon_plan_id) REFERENCES recon_plan (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS recon_result (
    id               VARCHAR(64) NOT NULL,
    run_id           VARCHAR(64) NOT NULL,
    status           VARCHAR(32) NOT NULL,
    left_record_id   VARCHAR(64),
    right_record_id  VARCHAR(64),
    recon_key        VARCHAR(255),
    differences      LONGTEXT,
    PRIMARY KEY (id),
    KEY idx_recon_result_run (run_id),
    KEY idx_recon_result_run_status (run_id, status),
    CONSTRAINT fk_recon_result_run
        FOREIGN KEY (run_id) REFERENCES recon_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS source (
    id                 VARCHAR(64)  NOT NULL,
    name               VARCHAR(255) NOT NULL,
    vendor             VARCHAR(128) NOT NULL,
    base_url           VARCHAR(512) NOT NULL,
    path               VARCHAR(255) NOT NULL,
    http_method        VARCHAR(8)   NOT NULL,
    param_schema_json  JSON         NOT NULL,
    record_path        VARCHAR(255),
    secret_ref         VARCHAR(128),
    auth_type          VARCHAR(16)  NOT NULL DEFAULT 'NONE',
    auth_header        VARCHAR(64),
    max_concurrent     INT          NOT NULL DEFAULT 2,
    enabled            TINYINT(1)   NOT NULL DEFAULT 1,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS collection (
    id                    VARCHAR(64)  NOT NULL,
    name                  VARCHAR(255) NOT NULL,
    owner_email           VARCHAR(320) NOT NULL,
    plan_id               VARCHAR(64)  NOT NULL,
    left_source_id        VARCHAR(64)  NOT NULL,
    right_source_id       VARCHAR(64)  NOT NULL,
    left_identity_param   VARCHAR(64)  NOT NULL,
    right_identity_param  VARCHAR(64)  NOT NULL,
    left_date_param       VARCHAR(64)  NOT NULL,
    right_date_param      VARCHAR(64)  NOT NULL,
    constant_params_json  JSON,
    date_policy           VARCHAR(16)  NOT NULL,
    schedule_cron         VARCHAR(64),
    created_at            DATETIME(6)  NOT NULL,
    updated_at            DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_collection_owner (owner_email),
    CONSTRAINT fk_collection_plan FOREIGN KEY (plan_id) REFERENCES recon_plan (id),
    CONSTRAINT fk_collection_left_source FOREIGN KEY (left_source_id) REFERENCES source (id),
    CONSTRAINT fk_collection_right_source FOREIGN KEY (right_source_id) REFERENCES source (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS collection_member (
    id             VARCHAR(64)  NOT NULL,
    collection_id  VARCHAR(64)  NOT NULL,
    email          VARCHAR(320) NOT NULL,
    role           VARCHAR(16)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_collection_member (collection_id, email),
    CONSTRAINT fk_collection_member_collection FOREIGN KEY (collection_id) REFERENCES collection (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS collection_pair (
    id             VARCHAR(64)  NOT NULL,
    collection_id  VARCHAR(64)  NOT NULL,
    left_value     VARCHAR(128) NOT NULL,
    right_value    VARCHAR(128) NOT NULL,
    sort_order     INT,
    PRIMARY KEY (id),
    UNIQUE KEY uq_collection_pair_left (collection_id, left_value),
    UNIQUE KEY uq_collection_pair_right (collection_id, right_value),
    CONSTRAINT fk_collection_pair_collection FOREIGN KEY (collection_id) REFERENCES collection (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS collection_cycle (
    id                   VARCHAR(64) NOT NULL,
    collection_id        VARCHAR(64) NOT NULL,
    as_of_date           DATE        NOT NULL,
    status               VARCHAR(32) NOT NULL,
    job_id               VARCHAR(64),
    item_count           BIGINT,
    failed_fetch_count   BIGINT,
    break_fund_count     BIGINT,
    started_at           DATETIME(6),
    completed_at         DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_collection_cycle_date (collection_id, as_of_date),
    CONSTRAINT fk_collection_cycle_collection FOREIGN KEY (collection_id) REFERENCES collection (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS collection_item (
    id                 VARCHAR(64)  NOT NULL,
    cycle_id           VARCHAR(64)  NOT NULL,
    pair_id            VARCHAR(64)  NOT NULL,
    left_value         VARCHAR(128) NOT NULL,
    right_value        VARCHAR(128) NOT NULL,
    left_dataset_id    VARCHAR(64),
    right_dataset_id   VARCHAR(64),
    recon_run_id       VARCHAR(64),
    fetch_status       VARCHAR(32)  NOT NULL,
    recon_status       VARCHAR(32),
    matched_count      BIGINT,
    break_count        BIGINT,
    only_in_left_count BIGINT,
    only_in_right_count BIGINT,
    error_message      VARCHAR(1024),
    PRIMARY KEY (id),
    UNIQUE KEY uq_collection_item_pair (cycle_id, pair_id),
    KEY idx_collection_item_run (recon_run_id),
    CONSTRAINT fk_collection_item_cycle FOREIGN KEY (cycle_id) REFERENCES collection_cycle (id),
    CONSTRAINT fk_collection_item_pair FOREIGN KEY (pair_id) REFERENCES collection_pair (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
