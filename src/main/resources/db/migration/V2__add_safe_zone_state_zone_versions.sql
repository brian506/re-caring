SET LOCAL lock_timeout = '5s';

ALTER TABLE safe_zone_states ADD COLUMN zone_versions TEXT;
