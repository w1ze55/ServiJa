-- Remove o valor informado pelo cliente na solicitacao.
-- O valor negociado passa a existir apenas na resposta do freelancer.

SET @drop_chk_budget_min = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = DATABASE()
              AND TABLE_NAME = 'service_requests'
              AND CONSTRAINT_NAME = 'chk_budget_min'
        ),
        'ALTER TABLE service_requests DROP CHECK chk_budget_min',
        'SELECT 1'
    )
);
PREPARE stmt FROM @drop_chk_budget_min;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_chk_budget_max = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = DATABASE()
              AND TABLE_NAME = 'service_requests'
              AND CONSTRAINT_NAME = 'chk_budget_max'
        ),
        'ALTER TABLE service_requests DROP CHECK chk_budget_max',
        'SELECT 1'
    )
);
PREPARE stmt FROM @drop_chk_budget_max;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_budget_column = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'service_requests'
              AND COLUMN_NAME = 'budget'
        ),
        'ALTER TABLE service_requests DROP COLUMN budget',
        'SELECT 1'
    )
);
PREPARE stmt FROM @drop_budget_column;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
