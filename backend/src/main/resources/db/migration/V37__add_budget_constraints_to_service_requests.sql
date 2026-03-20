-- Primeiro, atualiza registros existentes que violam as constraints
-- Define orçamento mínimo de R$ 5,00 para registros com valores menores
UPDATE service_requests SET budget = 5.00 WHERE budget < 5.00;

-- Define orçamento máximo de R$ 1.000.000,00 para registros com valores maiores
UPDATE service_requests SET budget = 1000000.00 WHERE budget > 1000000.00;

-- Agora adiciona as constraints de check para orçamento mínimo e máximo
ALTER TABLE service_requests
    ADD CONSTRAINT chk_budget_min CHECK (budget >= 5.00),
    ADD CONSTRAINT chk_budget_max CHECK (budget <= 1000000.00);

