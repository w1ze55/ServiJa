UPDATE services
SET work_model = UPPER(TRIM(work_model))
WHERE work_model IS NOT NULL;

UPDATE services
SET work_model = 'REMOTO'
WHERE work_model IS NULL
   OR work_model = ''
   OR work_model NOT IN ('PRESENCIAL', 'REMOTO', 'HIBRIDO');

ALTER TABLE services
    MODIFY COLUMN work_model ENUM('PRESENCIAL', 'REMOTO', 'HIBRIDO') NOT NULL DEFAULT 'REMOTO';
