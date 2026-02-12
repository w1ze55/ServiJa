-- Adicionar campos de geolocalização para freelancers
ALTER TABLE freelancers
ADD COLUMN latitude DECIMAL(10, 8) NULL,
ADD COLUMN longitude DECIMAL(11, 8) NULL;

-- Criar índice para buscas por localização
CREATE INDEX idx_freelancers_location ON freelancers (latitude, longitude);
