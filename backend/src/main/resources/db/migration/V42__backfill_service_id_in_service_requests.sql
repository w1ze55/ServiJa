-- Backfill service_id for existing service_requests that were created from
-- a service detail page. The frontend sets service_name as "{title} - Solicitação".
UPDATE service_requests sr
JOIN services s ON sr.freelancer_id = s.freelancer_id
  AND sr.service_name = CONCAT(s.title, ' - Solicitação')
SET sr.service_id = s.id
WHERE sr.service_id IS NULL;

-- Also match records where service_name equals the service title directly
UPDATE service_requests sr
JOIN services s ON sr.freelancer_id = s.freelancer_id
  AND sr.service_name = s.title
SET sr.service_id = s.id
WHERE sr.service_id IS NULL;
