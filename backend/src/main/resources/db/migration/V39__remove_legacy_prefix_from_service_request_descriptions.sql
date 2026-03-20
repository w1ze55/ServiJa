-- Remove prefixo legado inserido no início da descrição das solicitações
UPDATE service_requests
SET description = REGEXP_REPLACE(
    description,
    '^\\[Solicitação referente ao serviço: [^]]+\\][[:space:]]*',
    ''
)
WHERE description REGEXP '^\\[Solicitação referente ao serviço: [^]]+\\]';
