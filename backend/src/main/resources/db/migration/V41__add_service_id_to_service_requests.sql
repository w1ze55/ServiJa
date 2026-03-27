ALTER TABLE service_requests ADD COLUMN service_id BIGINT NULL;
ALTER TABLE service_requests ADD CONSTRAINT fk_service_request_service
  FOREIGN KEY (service_id) REFERENCES services(id);
