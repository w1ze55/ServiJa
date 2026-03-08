-- Adicionar o valor 'DELIVERED' ao ENUM da coluna status na tabela service_requests
ALTER TABLE service_requests
MODIFY COLUMN status ENUM('PENDING', 'SENT', 'NEGOTIATING', 'ACCEPTED', 'REJECTED', 'IN_PROGRESS', 'DELIVERED', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING';
