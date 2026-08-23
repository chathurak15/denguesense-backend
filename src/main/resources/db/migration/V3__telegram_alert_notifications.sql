-- Telegram alert audit: notification rows for every PHI in a cluster's district,
-- plus one Telegram chat binding per user.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS telegram_registration_code VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_telegram_registration_code
    ON users (telegram_registration_code)
    WHERE telegram_registration_code IS NOT NULL;

CREATE TABLE IF NOT EXISTS notification (
    id             BIGSERIAL PRIMARY KEY,
    channel        VARCHAR(20)  NOT NULL,
    reference_type VARCHAR(20)  NOT NULL,
    reference_id   BIGINT       NOT NULL,
    recipient      VARCHAR(255) NOT NULL,
    message_body   TEXT         NOT NULL,
    status         VARCHAR(40)  NOT NULL,
    failure_reason VARCHAR(1000),
    created_at     TIMESTAMP,
    sent_at        TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_reference
    ON notification (reference_type, reference_id);

CREATE INDEX IF NOT EXISTS idx_notification_status
    ON notification (status);

CREATE TABLE IF NOT EXISTS telegram_registration (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    chat_id       VARCHAR(255) NOT NULL,
    district_id   BIGINT,
    registered_at TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_telegram_registration_user'
    ) THEN
        ALTER TABLE telegram_registration
            ADD CONSTRAINT uq_telegram_registration_user UNIQUE (user_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_telegram_registration_user'
    ) THEN
        ALTER TABLE telegram_registration
            ADD CONSTRAINT fk_telegram_registration_user
            FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
    END IF;
END $$;
