-- Regra de negócio: preço de serviço ATIVO entre R$ 40,00 e R$ 1.000.000,00.
-- Serviços fora do range são desativados (preservando o preço original) em vez de ter o valor alterado;
-- para voltar a ficar ativos, o freelancer precisa ajustar o preço para dentro do range.

-- Desativa serviços existentes com preço fora do range (mantém o valor original)
UPDATE services SET is_active = FALSE WHERE price < 40.00 OR price > 1000000.00;

-- Constraint condicional: só exige o range em serviços ativos.
-- Serviços inativos podem manter qualquer preço (inclusive os legados fora do range).
ALTER TABLE services
    ADD CONSTRAINT chk_service_price_active CHECK (
        is_active = FALSE OR (price >= 40.00 AND price <= 1000000.00)
    );
