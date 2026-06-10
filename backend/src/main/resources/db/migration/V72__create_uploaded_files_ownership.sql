-- H3: rastreia o dono de cada objeto enviado ao S3 para autorizar exclusões
-- e impedir deleção arbitrária de arquivos de terceiros via URL.
CREATE TABLE uploaded_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    s3_key VARCHAR(512) NOT NULL,
    owner_email VARCHAR(256) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT uq_uploaded_files_s3_key UNIQUE (s3_key)
);

CREATE INDEX idx_uploaded_files_owner_email ON uploaded_files (owner_email);
