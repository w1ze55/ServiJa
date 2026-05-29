-- V60__drop_kyc_from_freelancers.sql
-- Remoção da funcionalidade de KYC: drop do índice e coluna kyc_completed

DROP INDEX idx_freelancers_kyc_completed ON freelancers;
ALTER TABLE freelancers DROP COLUMN kyc_completed;
