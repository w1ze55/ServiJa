ALTER TABLE service_requests
    ADD COLUMN response_milestones LONGTEXT NULL AFTER response_attachments;
