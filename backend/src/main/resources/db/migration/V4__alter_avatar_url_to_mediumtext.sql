-- V5__alter_avatar_url_to_mediumtext.sql
-- Altera o tipo da coluna avatar_url de TEXT para MEDIUMTEXT para suportar imagens maiores em base64

ALTER TABLE users MODIFY COLUMN avatar_url MEDIUMTEXT;

