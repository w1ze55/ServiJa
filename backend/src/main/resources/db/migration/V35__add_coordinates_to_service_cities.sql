ALTER TABLE service_cities
ADD COLUMN zip_code VARCHAR(10) NULL,
ADD COLUMN latitude DECIMAL(10, 8) NULL,
ADD COLUMN longitude DECIMAL(11, 8) NULL;

CREATE INDEX idx_service_cities_coordinates ON service_cities (latitude, longitude);
