-- Ensure every service has a work model before removing the legacy freelancer field
UPDATE services
SET work_model = 'REMOTO'
WHERE work_model IS NULL;

ALTER TABLE services
MODIFY COLUMN work_model VARCHAR(20) NOT NULL DEFAULT 'REMOTO';

-- Remove the legacy field from freelancers
ALTER TABLE freelancers
DROP COLUMN work_model;
