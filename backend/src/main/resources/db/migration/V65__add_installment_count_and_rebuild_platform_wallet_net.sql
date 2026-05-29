ALTER TABLE payments
ADD COLUMN installment_count INT NULL AFTER payment_method;

UPDATE payments
SET installment_count = 1
WHERE LOWER(payment_method) = 'credit-card'
  AND installment_count IS NULL;

INSERT INTO platform_wallets (
    id,
    available_balance,
    reserved_balance,
    total_earned,
    total_withdrawn
)
SELECT
    1,
    totals.total_earned - totals.reserved_balance - totals.total_withdrawn,
    totals.reserved_balance,
    totals.total_earned,
    totals.total_withdrawn
FROM (
    SELECT
        COALESCE((
            SELECT SUM(GREATEST(COALESCE(p.platform_fee, 0) - COALESCE(p.gateway_fee, 0), 0))
            FROM payments p
            WHERE p.platform_fee_released_at IS NOT NULL
        ), 0) AS total_earned,
        COALESCE((
            SELECT SUM(w.amount)
            FROM platform_withdrawal_requests w
            WHERE w.status IN ('PENDING', 'PROCESSING')
        ), 0) AS reserved_balance,
        COALESCE((
            SELECT SUM(w.amount)
            FROM platform_withdrawal_requests w
            WHERE w.status = 'COMPLETED'
        ), 0) AS total_withdrawn
) AS totals
ON DUPLICATE KEY UPDATE
    available_balance = VALUES(available_balance),
    reserved_balance = VALUES(reserved_balance),
    total_earned = VALUES(total_earned),
    total_withdrawn = VALUES(total_withdrawn);
