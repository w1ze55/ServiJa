-- V5__add_freelancer_profile_fields.sql
-- Adicionar novos campos ao perfil do freelancer

ALTER TABLE freelancers
ADD COLUMN category VARCHAR(100),
ADD COLUMN hourly_rate DECIMAL(10, 2),
ADD COLUMN service_type VARCHAR(50),
ADD COLUMN cities TEXT,
ADD COLUMN social_links TEXT;

-- Índice para categoria
CREATE INDEX idx_freelancers_category ON freelancers(category);

