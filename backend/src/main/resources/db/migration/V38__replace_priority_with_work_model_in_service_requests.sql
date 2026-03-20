-- Persistir modelo de trabalho na solicitação e remover prioridade
ALTER TABLE service_requests
ADD COLUMN work_model ENUM('PRESENCIAL', 'REMOTO', 'HIBRIDO') NULL AFTER deadline;

-- Backfill para solicitações vinculadas a um serviço conhecido
UPDATE service_requests sr
JOIN services s
  ON s.freelancer_id = sr.freelancer_id
 AND (
      sr.service_name = s.title
      OR sr.service_name = CONCAT(s.title, ' - Solicitação')
 )
SET sr.work_model = s.work_model
WHERE sr.work_model IS NULL
  AND s.work_model IS NOT NULL;

-- Fallback para registros antigos sem vínculo exato:
-- com endereço inferimos presencial; sem endereço inferimos remoto
UPDATE service_requests
SET work_model = CASE
    WHEN address IS NOT NULL AND TRIM(address) <> '' THEN 'PRESENCIAL'
    ELSE 'REMOTO'
END
WHERE work_model IS NULL;

ALTER TABLE service_requests
MODIFY COLUMN work_model ENUM('PRESENCIAL', 'REMOTO', 'HIBRIDO') NOT NULL;

ALTER TABLE service_requests
DROP COLUMN priority;
