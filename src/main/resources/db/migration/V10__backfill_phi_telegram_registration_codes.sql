-- Existing PHI accounts were created before codes were assigned on register.
UPDATE users
SET telegram_registration_code = 'PHI-' || upper(substr(md5(random()::text || id::text), 1, 8))
WHERE role = 'PHI'
  AND telegram_registration_code IS NULL;
