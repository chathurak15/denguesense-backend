-- Report clustering persistence: report_cluster + cluster_membership
-- Safe to re-run against databases whose schema was previously managed by Hibernate ddl-auto.

CREATE TABLE IF NOT EXISTS report_cluster (
    id           BIGSERIAL PRIMARY KEY,
    district_id  BIGINT      NOT NULL,
    report_count INTEGER     NOT NULL DEFAULT 0,
    status       VARCHAR(20) NOT NULL,
    detected_at  TIMESTAMP   NOT NULL,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP,
    version      BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_cluster_district_status
    ON report_cluster (district_id, status);

-- At most one ACTIVE cluster per district (idempotency under concurrent detectors).
CREATE UNIQUE INDEX IF NOT EXISTS uq_report_cluster_one_active_per_district
    ON report_cluster (district_id)
    WHERE status = 'ACTIVE';

CREATE TABLE IF NOT EXISTS cluster_membership (
    id         BIGSERIAL PRIMARY KEY,
    cluster_id BIGINT NOT NULL,
    report_id  BIGINT NOT NULL,
    added_at   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_membership_cluster
    ON cluster_membership (cluster_id);

CREATE INDEX IF NOT EXISTS idx_membership_report
    ON cluster_membership (report_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_cluster_report'
    ) AND NOT EXISTS (
        SELECT 1 FROM pg_class WHERE relname = 'uq_cluster_report'
    ) THEN
        ALTER TABLE cluster_membership
            ADD CONSTRAINT uq_cluster_report UNIQUE (cluster_id, report_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_membership_cluster'
    ) THEN
        ALTER TABLE cluster_membership
            ADD CONSTRAINT fk_membership_cluster
            FOREIGN KEY (cluster_id) REFERENCES report_cluster (id) ON DELETE CASCADE;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'reports'
    ) AND NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_membership_report'
    ) THEN
        ALTER TABLE cluster_membership
            ADD CONSTRAINT fk_membership_report
            FOREIGN KEY (report_id) REFERENCES reports (id);
    END IF;
END $$;

UPDATE report_cluster SET version = 0 WHERE version IS NULL;
