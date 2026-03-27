-- Adicionar o valor 'URGENT' ao ENUM da coluna priority
ALTER TABLE service_requests
MODIFY COLUMN priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') NOT NULL DEFAULT 'MEDIUM';
