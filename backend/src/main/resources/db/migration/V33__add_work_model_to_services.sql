-- Adiciona modelo de trabalho por serviço
ALTER TABLE services
ADD COLUMN work_model VARCHAR(20) NULL AFTER delivery_unit;

-- Replica o modelo de trabalho atual do freelancer para os serviços já existentes
UPDATE services s
JOIN freelancers f ON f.user_id = s.freelancer_id
SET s.work_model = f.work_model
WHERE s.work_model IS NULL
  AND f.work_model IS NOT NULL;
