-- V7: Seed district.rdhs_model_id to match LSTM LabelEncoder (alphabetical 0-25)
-- Must stay aligned with training / FastAPI rdhs_id embedding lookup.

UPDATE district SET rdhs_model_id = 0  WHERE LOWER(name) = 'ampara';
UPDATE district SET rdhs_model_id = 1  WHERE LOWER(name) = 'anuradhapura';
UPDATE district SET rdhs_model_id = 2  WHERE LOWER(name) = 'badulla';
UPDATE district SET rdhs_model_id = 3  WHERE LOWER(name) = 'batticaloa';
UPDATE district SET rdhs_model_id = 4  WHERE LOWER(name) = 'colombo';
UPDATE district SET rdhs_model_id = 5  WHERE LOWER(name) = 'galle';
UPDATE district SET rdhs_model_id = 6  WHERE LOWER(name) = 'gampaha';
UPDATE district SET rdhs_model_id = 7  WHERE LOWER(name) = 'hambantota';
UPDATE district SET rdhs_model_id = 8  WHERE LOWER(name) = 'jaffna';
UPDATE district SET rdhs_model_id = 9  WHERE LOWER(name) = 'kalmunai' OR LOWER(rdhs_zone) = 'kalmunai';
UPDATE district SET rdhs_model_id = 10 WHERE LOWER(name) = 'kalutara';
UPDATE district SET rdhs_model_id = 11 WHERE LOWER(name) = 'kandy';
UPDATE district SET rdhs_model_id = 12 WHERE LOWER(name) = 'kegalle';
UPDATE district SET rdhs_model_id = 13 WHERE LOWER(name) = 'kilinochchi';
UPDATE district SET rdhs_model_id = 14 WHERE LOWER(name) = 'kurunegala';
UPDATE district SET rdhs_model_id = 15 WHERE LOWER(name) = 'mannar';
UPDATE district SET rdhs_model_id = 16 WHERE LOWER(name) = 'matale';
UPDATE district SET rdhs_model_id = 17 WHERE LOWER(name) = 'matara';
UPDATE district SET rdhs_model_id = 18 WHERE LOWER(name) = 'monaragala';
UPDATE district SET rdhs_model_id = 19 WHERE LOWER(name) = 'mullaitivu';
UPDATE district SET rdhs_model_id = 20 WHERE LOWER(name) = 'nuwara eliya';
UPDATE district SET rdhs_model_id = 21 WHERE LOWER(name) = 'polonnaruwa';
UPDATE district SET rdhs_model_id = 22 WHERE LOWER(name) = 'puttalam';
UPDATE district SET rdhs_model_id = 23 WHERE LOWER(name) = 'ratnapura';
UPDATE district SET rdhs_model_id = 24 WHERE LOWER(name) = 'trincomalee';
UPDATE district SET rdhs_model_id = 25 WHERE LOWER(name) = 'vavuniya';
