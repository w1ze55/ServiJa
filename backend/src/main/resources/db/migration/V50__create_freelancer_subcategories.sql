CREATE TABLE freelancer_subcategories (
    freelancer_id BIGINT NOT NULL,
    subcategory_id BIGINT NOT NULL,
    PRIMARY KEY (freelancer_id, subcategory_id),
    CONSTRAINT fk_freelancer_subcategories_freelancer FOREIGN KEY (freelancer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_freelancer_subcategories_subcategory FOREIGN KEY (subcategory_id) REFERENCES service_subcategories (id) ON DELETE CASCADE
);

INSERT IGNORE INTO freelancer_subcategories (freelancer_id, subcategory_id)
SELECT user_id, subcategory_id
FROM freelancers
WHERE subcategory_id IS NOT NULL;
