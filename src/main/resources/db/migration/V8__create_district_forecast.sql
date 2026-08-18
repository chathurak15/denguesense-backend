-- V8: Trigger-layer forecast store.
-- One idempotently-upserted row per (RDHS district, target week) holding the 4-week horizon
-- vectors plus generation status/source for the PHI dashboard read path.
-- rdhs_id matches district.rdhs_model_id (LSTM LabelEncoder ids, 0-25, seeded in V7).

CREATE TABLE IF NOT EXISTS district_forecast (
    id                BIGSERIAL          PRIMARY KEY,
    rdhs_id           INTEGER            NOT NULL,
    district_name     VARCHAR(255)       NOT NULL,
    target_week_start DATE               NOT NULL,
    predictions       DOUBLE PRECISION[] NOT NULL,
    lower_bounds      DOUBLE PRECISION[] NOT NULL,
    upper_bounds      DOUBLE PRECISION[] NOT NULL,
    model_version     VARCHAR(50)        NOT NULL,
    status            VARCHAR(20)        NOT NULL,
    generation_source VARCHAR(20)        NOT NULL,
    generated_at      TIMESTAMPTZ        NOT NULL,
    version           BIGINT             NOT NULL DEFAULT 0,
    CONSTRAINT uq_district_forecast_rdhs_week UNIQUE (rdhs_id, target_week_start)
);

CREATE INDEX IF NOT EXISTS idx_district_forecast_rdhs_week
    ON district_forecast (rdhs_id, target_week_start);
