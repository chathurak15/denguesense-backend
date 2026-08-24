-- Hibernate ddl-auto created report_cluster_status_check from an older ClusterStatus
-- set that omitted ALERTED. TelegramAlertService then fails ACTIVE → ALERTED after a
-- successful send (constraint report_cluster_status_check).

ALTER TABLE report_cluster DROP CONSTRAINT IF EXISTS report_cluster_status_check;

ALTER TABLE report_cluster
    ADD CONSTRAINT report_cluster_status_check
    CHECK (status IN ('ACTIVE', 'ALERTED', 'CLEARED', 'EXPIRED'));

-- Clusters that already got a SENT Telegram row stayed ACTIVE because the check rejected
-- ALERTED. alerted_at is also set, so ClusterDetectedEvent will not fire again.
UPDATE report_cluster rc
SET status = 'ALERTED'
WHERE rc.status = 'ACTIVE'
  AND rc.alerted_at IS NOT NULL
  AND EXISTS (
      SELECT 1
      FROM notification n
      WHERE n.reference_type = 'CLUSTER'
        AND n.reference_id = rc.id
        AND n.channel = 'TELEGRAM'
        AND n.status = 'SENT'
  );
