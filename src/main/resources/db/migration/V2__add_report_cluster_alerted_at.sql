-- One-shot alert marker: scheduler re-runs reuse the ACTIVE cluster and skip
-- ClusterDetectedEvent once alerted_at is set.
ALTER TABLE report_cluster
    ADD COLUMN IF NOT EXISTS alerted_at TIMESTAMP;
