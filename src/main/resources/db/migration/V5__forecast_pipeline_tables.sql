-- V5: Forecast pipeline tables and district reference columns
-- Adds: weather_records, dengue_case_records, bsds_weekly, dengue_forecasts
-- Extends: district with LSTM model reference fields

-- District forecast reference columns (seeded from training-time mapping)
ALTER TABLE district ADD COLUMN IF NOT EXISTS rdhs_model_id INTEGER;
ALTER TABLE district ADD COLUMN IF NOT EXISTS zone_dry_zone BOOLEAN;
ALTER TABLE district ADD COLUMN IF NOT EXISTS zone_intermediate_zone BOOLEAN;
ALTER TABLE district ADD COLUMN IF NOT EXISTS zone_wet_zone BOOLEAN;
ALTER TABLE district ADD COLUMN IF NOT EXISTS population_density DOUBLE PRECISION;

-- Weekly weather observations per district
CREATE TABLE IF NOT EXISTS weather_records (
    id              BIGSERIAL PRIMARY KEY,
    district_id     BIGINT           NOT NULL REFERENCES district(id),
    week_start_date DATE             NOT NULL,
    week_end_date   DATE             NOT NULL,
    temp_mean       DOUBLE PRECISION,
    temp_max        DOUBLE PRECISION,
    temp_min        DOUBLE PRECISION,
    rainfall_mm     DOUBLE PRECISION,
    humidity_pct    DOUBLE PRECISION,
    CONSTRAINT uq_weather_district_week UNIQUE (district_id, week_start_date)
);

CREATE INDEX IF NOT EXISTS idx_weather_district_date
    ON weather_records (district_id, week_start_date);

-- Weekly dengue case records per district
CREATE TABLE IF NOT EXISTS dengue_case_records (
    id                BIGSERIAL PRIMARY KEY,
    district_id       BIGINT           NOT NULL REFERENCES district(id),
    week_start_date   DATE             NOT NULL,
    week_end_date     DATE             NOT NULL,
    week_cases        INTEGER,
    cumulative_cases  INTEGER,
    week_cases_scaled DOUBLE PRECISION,
    CONSTRAINT uq_cases_district_week UNIQUE (district_id, week_start_date)
);

CREATE INDEX IF NOT EXISTS idx_cases_district_date
    ON dengue_case_records (district_id, week_start_date);

-- Weekly Breeding Site Density Score (aggregated from citizen reports)
-- Kept separate from the LSTM forecast pipeline; used by the risk-clustering stream.
CREATE TABLE IF NOT EXISTS bsds_weekly (
    id              BIGSERIAL PRIMARY KEY,
    district_id     BIGINT           NOT NULL REFERENCES district(id),
    week_start_date DATE             NOT NULL,
    week_end_date   DATE             NOT NULL,
    bsds_score      DOUBLE PRECISION NOT NULL,
    report_count    INTEGER,
    CONSTRAINT uq_bsds_district_week UNIQUE (district_id, week_start_date)
);

CREATE INDEX IF NOT EXISTS idx_bsds_district_date
    ON bsds_weekly (district_id, week_start_date);

-- Persisted LSTM forecast outputs (4-week-ahead predictions)
CREATE TABLE IF NOT EXISTS dengue_forecasts (
    id              BIGSERIAL PRIMARY KEY,
    district_id     BIGINT           NOT NULL REFERENCES district(id),
    forecast_date   DATE             NOT NULL,
    target_date     DATE             NOT NULL,
    predicted_cases DOUBLE PRECISION NOT NULL,
    lower_bound     DOUBLE PRECISION,
    upper_bound     DOUBLE PRECISION,
    model_version   VARCHAR(50)      NOT NULL,
    created_at      TIMESTAMP,
    CONSTRAINT uq_forecast_district_target_model UNIQUE (district_id, target_date, model_version)
);

CREATE INDEX IF NOT EXISTS idx_forecast_district_target
    ON dengue_forecasts (district_id, target_date);

CREATE INDEX IF NOT EXISTS idx_forecast_date
    ON dengue_forecasts (forecast_date);
