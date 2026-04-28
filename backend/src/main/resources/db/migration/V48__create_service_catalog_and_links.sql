CREATE TABLE service_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slug VARCHAR(120) NOT NULL,
    name VARCHAR(150) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    active BIT NOT NULL DEFAULT b'1',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_service_categories_slug UNIQUE (slug)
);

CREATE TABLE service_subcategories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    slug VARCHAR(160) NOT NULL,
    name VARCHAR(160) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    active BIT NOT NULL DEFAULT b'1',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_service_subcategories_slug UNIQUE (slug),
    CONSTRAINT fk_service_subcategories_category FOREIGN KEY (category_id) REFERENCES service_categories (id)
);

ALTER TABLE services
    ADD COLUMN category_id BIGINT NULL,
    ADD COLUMN subcategory_id BIGINT NULL,
    ADD CONSTRAINT fk_services_category FOREIGN KEY (category_id) REFERENCES service_categories (id),
    ADD CONSTRAINT fk_services_subcategory FOREIGN KEY (subcategory_id) REFERENCES service_subcategories (id);

ALTER TABLE freelancers
    ADD COLUMN category_id BIGINT NULL,
    ADD COLUMN subcategory_id BIGINT NULL,
    ADD CONSTRAINT fk_freelancers_category FOREIGN KEY (category_id) REFERENCES service_categories (id),
    ADD CONSTRAINT fk_freelancers_subcategory FOREIGN KEY (subcategory_id) REFERENCES service_subcategories (id);

ALTER TABLE portfolio_projects
    ADD COLUMN category_id BIGINT NULL,
    ADD COLUMN subcategory_id BIGINT NULL,
    ADD CONSTRAINT fk_portfolio_projects_category FOREIGN KEY (category_id) REFERENCES service_categories (id),
    ADD CONSTRAINT fk_portfolio_projects_subcategory FOREIGN KEY (subcategory_id) REFERENCES service_subcategories (id);

ALTER TABLE service_requests
    ADD COLUMN subcategory VARCHAR(160) NULL,
    ADD COLUMN category_id BIGINT NULL,
    ADD COLUMN subcategory_id BIGINT NULL,
    ADD CONSTRAINT fk_service_requests_category FOREIGN KEY (category_id) REFERENCES service_categories (id),
    ADD CONSTRAINT fk_service_requests_subcategory FOREIGN KEY (subcategory_id) REFERENCES service_subcategories (id);

CREATE INDEX idx_services_category_id ON services (category_id);
CREATE INDEX idx_services_subcategory_id ON services (subcategory_id);
CREATE INDEX idx_freelancers_category_id ON freelancers (category_id);
CREATE INDEX idx_freelancers_subcategory_id ON freelancers (subcategory_id);
CREATE INDEX idx_portfolio_projects_category_id ON portfolio_projects (category_id);
CREATE INDEX idx_portfolio_projects_subcategory_id ON portfolio_projects (subcategory_id);
CREATE INDEX idx_service_requests_category_id ON service_requests (category_id);
CREATE INDEX idx_service_requests_subcategory_id ON service_requests (subcategory_id);
