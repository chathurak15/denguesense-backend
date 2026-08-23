-- Citizen FCM token, cluster status rename RESOLVED → CLEARED, live-cluster uniqueness.

ALTER TABLE reports
    ADD COLUMN IF NOT EXISTS fcm_device_token VARCHAR(512);

UPDATE report_cluster
SET status = 'CLEARED'
WHERE status = 'RESOLVED';

DROP INDEX IF EXISTS uq_report_cluster_one_active_per_district;

CREATE UNIQUE INDEX IF NOT EXISTS uq_report_cluster_one_live_per_district
    ON report_cluster (district_id)
    WHERE status IN ('ACTIVE', 'ALERTED');
