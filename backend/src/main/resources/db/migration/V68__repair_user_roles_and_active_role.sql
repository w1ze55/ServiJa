UPDATE users
SET active_role = user_type
WHERE active_role IS NULL
   OR (active_role = 'ADMIN' AND user_type <> 'ADMIN');

DELETE ur
FROM user_roles ur
JOIN users u ON u.id = ur.user_id
WHERE ur.role = 'ADMIN'
  AND u.user_type <> 'ADMIN';

INSERT IGNORE INTO user_roles (user_id, role)
SELECT id, user_type
FROM users
WHERE user_type IS NOT NULL;

INSERT IGNORE INTO user_roles (user_id, role)
SELECT user_id, 'CONTRACTOR'
FROM contractors;

INSERT IGNORE INTO user_roles (user_id, role)
SELECT user_id, 'FREELANCER'
FROM freelancers;
