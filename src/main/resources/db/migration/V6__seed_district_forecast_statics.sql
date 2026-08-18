-- V6: Seed district LSTM static features
-- population_density = 2025 DCS population / area_km2, rounded to 1 decimal
--   (same formula as dengue_sense_lk/scripts/merge_population_carryover.py)
--   Ampara/Kalmunai split: Kalmunai = 40% of Ampara total
-- climate zone one-hots from scratch_code/04_feature_engineering_and_encoding.py

UPDATE district SET
    population_density = 101.2,
    zone_dry_zone = TRUE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = FALSE
WHERE LOWER(name) = 'ampara';

UPDATE district SET
    population_density = 133.6,
    zone_dry_zone = TRUE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = FALSE
WHERE LOWER(name) = 'anuradhapura';

UPDATE district SET
    population_density = 305.1,
    zone_dry_zone = FALSE,
    zone_intermediate_zone = TRUE,
    zone_wet_zone = FALSE
WHERE LOWER(name) = 'badulla';

UPDATE district SET
    population_density = 208.8,
    zone_dry_zone = TRUE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = FALSE
WHERE LOWER(name) = 'batticaloa';

UPDATE district SET
    population_density = 3392.0,
    zone_dry_zone = FALSE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = TRUE
WHERE LOWER(name) = 'colombo';

UPDATE district SET
    population_density = 663.4,
    zone_dry_zone = FALSE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = TRUE
WHERE LOWER(name) = 'galle';

UPDATE district SET
    population_density = 1752.7,
    zone_dry_zone = FALSE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = TRUE
WHERE LOWER(name) = 'gampaha';

UPDATE district SET
    population_density = 257.2,
    zone_dry_zone = TRUE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = FALSE
WHERE LOWER(name) = 'hambantota';

UPDATE district SET
    population_density = 580.5,
    zone_dry_zone = TRUE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = FALSE
WHERE LOWER(name) = 'jaffna';

UPDATE district SET
    population_density = 168.7,
    zone_dry_zone = TRUE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = FALSE
WHERE LOWER(name) = 'kalmunai' OR LOWER(rdhs_zone) = 'kalmunai';

UPDATE district SET
    population_density = 814.8,
    zone_dry_zone = FALSE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = TRUE
WHERE LOWER(name) = 'kalutara';

UPDATE district SET
    population_density = 752.6,
    zone_dry_zone = FALSE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = TRUE
WHERE LOWER(name) = 'kandy';

UPDATE district SET
    population_density = 512.7,
    zone_dry_zone = FALSE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = TRUE
WHERE LOWER(name) = 'kegalle';

UPDATE district SET
    population_density = 107.1,
    zone_dry_zone = TRUE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = FALSE
WHERE LOWER(name) = 'kilinochchi';

UPDATE district SET
    population_density = 366.3,
    zone_dry_zone = FALSE,
    zone_intermediate_zone = TRUE,
    zone_wet_zone = FALSE
WHERE LOWER(name) = 'kurunegala';

UPDATE district SET
    population_density = 62.1,
    zone_dry_zone = TRUE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = FALSE
WHERE LOWER(name) = 'mannar';

UPDATE district SET
    population_density = 263.9,
    zone_dry_zone = FALSE,
    zone_intermediate_zone = TRUE,
    zone_wet_zone = FALSE
WHERE LOWER(name) = 'matale';

UPDATE district SET
    population_density = 651.6,
    zone_dry_zone = FALSE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = TRUE
WHERE LOWER(name) = 'matara';

UPDATE district SET
    population_density = 93.6,
    zone_dry_zone = FALSE,
    zone_intermediate_zone = TRUE,
    zone_wet_zone = FALSE
WHERE LOWER(name) = 'monaragala';

UPDATE district SET
    population_density = 48.9,
    zone_dry_zone = TRUE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = FALSE
WHERE LOWER(name) = 'mullaitivu';

UPDATE district SET
    population_density = 417.0,
    zone_dry_zone = FALSE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = TRUE
WHERE LOWER(name) = 'nuwara eliya';

UPDATE district SET
    population_density = 135.7,
    zone_dry_zone = TRUE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = FALSE
WHERE LOWER(name) = 'polonnaruwa';

UPDATE district SET
    population_density = 266.3,
    zone_dry_zone = TRUE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = FALSE
WHERE LOWER(name) = 'puttalam';

UPDATE district SET
    population_density = 349.3,
    zone_dry_zone = FALSE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = TRUE
WHERE LOWER(name) = 'ratnapura';

UPDATE district SET
    population_density = 162.4,
    zone_dry_zone = TRUE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = FALSE
WHERE LOWER(name) = 'trincomalee';

UPDATE district SET
    population_density = 88.0,
    zone_dry_zone = TRUE,
    zone_intermediate_zone = FALSE,
    zone_wet_zone = FALSE
WHERE LOWER(name) = 'vavuniya';
