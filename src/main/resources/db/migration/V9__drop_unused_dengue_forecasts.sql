-- V9: Drop unused per-week dengue_forecasts table.
-- Forecasts are stored on district_forecast (one row per RDHS + target week with 4-week vectors).

DROP INDEX IF EXISTS idx_forecast_date;
DROP INDEX IF EXISTS idx_forecast_district_target;
DROP TABLE IF EXISTS dengue_forecasts;
