-- Global Recon Service schema for Microsoft SQL Server
-- Run on an empty database, then set spring.jpa.hibernate.ddl-auto=none
-- Example: sqlcmd -S <server> -d <database> -i src/main/resources/db/schema-sqlserver.sql

IF OBJECT_ID(N'dbo.dataset', N'U') IS NULL
CREATE TABLE dbo.dataset (
    id                  VARCHAR(64)   NOT NULL,
    name                NVARCHAR(255) NOT NULL,
    original_filename   NVARCHAR(255) NOT NULL,
    format              VARCHAR(16)   NOT NULL,
    status              VARCHAR(32)   NOT NULL,
    row_count           BIGINT        NULL,
    error_message       NVARCHAR(1024) NULL,
    owner_email         VARCHAR(320)  NOT NULL,
    ingestion_notes     NVARCHAR(4000) NULL,
    record_path         VARCHAR(255)  NULL,
    created_at          DATETIME2(6)  NOT NULL,
    updated_at          DATETIME2(6)  NOT NULL,
    CONSTRAINT pk_dataset PRIMARY KEY (id)
);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_dataset_owner' AND object_id = OBJECT_ID(N'dbo.dataset'))
CREATE INDEX idx_dataset_owner ON dbo.dataset (owner_email);

IF OBJECT_ID(N'dbo.dataset_record', N'U') IS NULL
CREATE TABLE dbo.dataset_record (
    id          VARCHAR(64)    NOT NULL,
    dataset_id  VARCHAR(64)    NOT NULL,
    row_index   BIGINT         NOT NULL,
    payload     NVARCHAR(MAX)  NOT NULL,
    CONSTRAINT pk_dataset_record PRIMARY KEY (id),
    CONSTRAINT fk_dataset_record_dataset
        FOREIGN KEY (dataset_id) REFERENCES dbo.dataset (id)
);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_dataset_record_dataset' AND object_id = OBJECT_ID(N'dbo.dataset_record'))
CREATE INDEX idx_dataset_record_dataset ON dbo.dataset_record (dataset_id);

IF OBJECT_ID(N'dbo.dataset_column', N'U') IS NULL
CREATE TABLE dbo.dataset_column (
    id               VARCHAR(64)    NOT NULL,
    dataset_id       VARCHAR(64)    NOT NULL,
    column_name      NVARCHAR(255)  NOT NULL,
    detected_type    VARCHAR(16)    NOT NULL,
    null_count       BIGINT         NULL,
    null_percentage  FLOAT          NULL,
    distinct_count   BIGINT         NULL,
    unique_ratio     FLOAT          NULL,
    sample_values    NVARCHAR(MAX)  NULL,
    min_value        NVARCHAR(255)  NULL,
    max_value        NVARCHAR(255)  NULL,
    common_patterns  NVARCHAR(MAX)  NULL,
    ordinal_position INT            NULL,
    CONSTRAINT pk_dataset_column PRIMARY KEY (id),
    CONSTRAINT fk_dataset_column_dataset
        FOREIGN KEY (dataset_id) REFERENCES dbo.dataset (id)
);

IF OBJECT_ID(N'dbo.recon_plan', N'U') IS NULL
CREATE TABLE dbo.recon_plan (
    id                  VARCHAR(64)    NOT NULL,
    left_dataset_id     VARCHAR(64)    NOT NULL,
    right_dataset_id    VARCHAR(64)    NOT NULL,
    owner_email         VARCHAR(320)   NOT NULL,
    user_notes          NVARCHAR(MAX)  NULL,
    status              VARCHAR(32)    NOT NULL,
    overall_confidence  FLOAT          NULL,
    warnings            NVARCHAR(MAX)  NULL,
    created_at          DATETIME2(6)   NOT NULL,
    approved_at         DATETIME2(6)   NULL,
    CONSTRAINT pk_recon_plan PRIMARY KEY (id),
    CONSTRAINT fk_recon_plan_left_dataset
        FOREIGN KEY (left_dataset_id) REFERENCES dbo.dataset (id),
    CONSTRAINT fk_recon_plan_right_dataset
        FOREIGN KEY (right_dataset_id) REFERENCES dbo.dataset (id)
);

IF OBJECT_ID(N'dbo.recon_key_mapping', N'U') IS NULL
CREATE TABLE dbo.recon_key_mapping (
    id          VARCHAR(64)   NOT NULL,
    plan_id     VARCHAR(64)   NOT NULL,
    left_field  NVARCHAR(255) NOT NULL,
    right_field NVARCHAR(255) NOT NULL,
    confidence  FLOAT         NULL,
    sort_order  INT           NULL,
    CONSTRAINT pk_recon_key_mapping PRIMARY KEY (id),
    CONSTRAINT fk_recon_key_mapping_plan
        FOREIGN KEY (plan_id) REFERENCES dbo.recon_plan (id)
);

IF OBJECT_ID(N'dbo.recon_field_mapping', N'U') IS NULL
CREATE TABLE dbo.recon_field_mapping (
    id          VARCHAR(64)   NOT NULL,
    plan_id     VARCHAR(64)   NOT NULL,
    left_field  NVARCHAR(255) NOT NULL,
    right_field NVARCHAR(255) NOT NULL,
    match_type  VARCHAR(32)   NOT NULL,
    tolerance   DECIMAL(19, 8) NULL,
    confidence  FLOAT         NULL,
    sort_order  INT           NULL,
    included    BIT           NOT NULL CONSTRAINT df_recon_field_mapping_included DEFAULT (1),
    CONSTRAINT pk_recon_field_mapping PRIMARY KEY (id),
    CONSTRAINT fk_recon_field_mapping_plan
        FOREIGN KEY (plan_id) REFERENCES dbo.recon_plan (id)
);

IF OBJECT_ID(N'dbo.recon_job', N'U') IS NULL
CREATE TABLE dbo.recon_job (
    id               VARCHAR(64)    NOT NULL,
    type             VARCHAR(32)    NOT NULL,
    status           VARCHAR(32)    NOT NULL,
    left_dataset_id  VARCHAR(64)    NULL,
    right_dataset_id VARCHAR(64)    NULL,
    recon_plan_id    VARCHAR(64)    NULL,
    result_id        VARCHAR(64)    NULL,
    owner_email      VARCHAR(320)   NOT NULL,
    notes            NVARCHAR(4000) NULL,
    error_message    NVARCHAR(1024) NULL,
    created_at       DATETIME2(6)   NOT NULL,
    started_at       DATETIME2(6)   NULL,
    completed_at     DATETIME2(6)   NULL,
    CONSTRAINT pk_recon_job PRIMARY KEY (id)
);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_recon_job_status' AND object_id = OBJECT_ID(N'dbo.recon_job'))
CREATE INDEX idx_recon_job_status ON dbo.recon_job (status);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_recon_job_owner' AND object_id = OBJECT_ID(N'dbo.recon_job'))
CREATE INDEX idx_recon_job_owner ON dbo.recon_job (owner_email);

IF OBJECT_ID(N'dbo.recon_run', N'U') IS NULL
CREATE TABLE dbo.recon_run (
    id                    VARCHAR(64)    NOT NULL,
    recon_plan_id         VARCHAR(64)    NOT NULL,
    owner_email           VARCHAR(320)   NOT NULL,
    status                VARCHAR(32)    NOT NULL,
    matched_count         BIGINT         NULL,
    break_count           BIGINT         NULL,
    only_in_left_count    BIGINT         NULL,
    only_in_right_count   BIGINT         NULL,
    duplicate_left_count  BIGINT         NULL,
    duplicate_right_count BIGINT         NULL,
    ambiguous_count       BIGINT         NULL,
    processed_count       BIGINT         NULL,
    name                  NVARCHAR(255)  NULL,
    saved_at              DATETIME2(6)   NULL,
    error_message         NVARCHAR(1024) NULL,
    started_at            DATETIME2(6)   NOT NULL,
    completed_at          DATETIME2(6)   NULL,
    CONSTRAINT pk_recon_run PRIMARY KEY (id),
    CONSTRAINT fk_recon_run_plan
        FOREIGN KEY (recon_plan_id) REFERENCES dbo.recon_plan (id)
);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_recon_run_owner' AND object_id = OBJECT_ID(N'dbo.recon_run'))
CREATE INDEX idx_recon_run_owner ON dbo.recon_run (owner_email);

IF OBJECT_ID(N'dbo.recon_result', N'U') IS NULL
CREATE TABLE dbo.recon_result (
    id               VARCHAR(64)   NOT NULL,
    run_id           VARCHAR(64)   NOT NULL,
    status           VARCHAR(32)   NOT NULL,
    left_record_id   VARCHAR(64)   NULL,
    right_record_id  VARCHAR(64)   NULL,
    recon_key        NVARCHAR(255) NULL,
    differences      NVARCHAR(MAX) NULL,
    CONSTRAINT pk_recon_result PRIMARY KEY (id),
    CONSTRAINT fk_recon_result_run
        FOREIGN KEY (run_id) REFERENCES dbo.recon_run (id)
);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_recon_result_run' AND object_id = OBJECT_ID(N'dbo.recon_result'))
CREATE INDEX idx_recon_result_run ON dbo.recon_result (run_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_recon_result_run_status' AND object_id = OBJECT_ID(N'dbo.recon_result'))
CREATE INDEX idx_recon_result_run_status ON dbo.recon_result (run_id, status);
