ALTER TABLE service_requests
ADD COLUMN counter_value DECIMAL(10, 2),
ADD COLUMN counter_days INT,
ADD COLUMN counter_notes TEXT;
