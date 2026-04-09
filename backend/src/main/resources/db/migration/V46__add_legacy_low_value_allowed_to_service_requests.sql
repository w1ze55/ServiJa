ALTER TABLE service_requests
    ADD COLUMN legacy_low_value_allowed BOOLEAN NOT NULL DEFAULT FALSE AFTER counter_notes;

UPDATE service_requests
SET legacy_low_value_allowed = TRUE
WHERE status IN ('NEGOTIATING', 'ACCEPTED')
  AND (
    (response_value IS NOT NULL AND response_value < 20.00)
    OR (counter_value IS NOT NULL AND counter_value < 20.00)
  );
