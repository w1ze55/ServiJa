-- V73__limit_user_name_length.sql
-- Limita o nome do usuário a 100 caracteres para evitar strings excessivas no banco.

-- Trunca nomes existentes acima de 100 caracteres antes de reduzir a coluna
UPDATE users SET name = LEFT(name, 100) WHERE CHAR_LENGTH(name) > 100;

ALTER TABLE users MODIFY COLUMN name VARCHAR(100) NOT NULL;
