-- Pine Drink - Authorization management permissions

INSERT INTO ia_permission (id, code, name, module, description, status, created_at, created_by, updated_at, updated_by)
VALUES
    ('00000000-0000-0000-0000-000000000261', 'ROLE_VIEW', 'View roles', 'AUTHORIZATION', 'View role list for authorization management', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000262', 'PERMISSION_VIEW', 'View permissions', 'AUTHORIZATION', 'View permission list for authorization management', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000263', 'ROLE_PERMISSION_VIEW', 'View role permissions', 'AUTHORIZATION', 'View role-permission matrix', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000264', 'ROLE_PERMISSION_UPDATE', 'Update role permissions', 'AUTHORIZATION', 'Update role-permission assignments', 'ACTIVE', NOW(), NULL, NOW(), NULL)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    module = VALUES(module),
    description = VALUES(description),
    status = VALUES(status),
    updated_at = NOW();

-- ADMIN receives authorization-management permissions. ADMIN role itself remains protected from runtime permission updates.
INSERT INTO ia_role_permission (id, role_id, permission_id, status, created_at, created_by, updated_at, updated_by)
VALUES
    ('00000000-0000-0000-0000-000000000361', '00000000-0000-0000-0000-000000000020', '00000000-0000-0000-0000-000000000261', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000362', '00000000-0000-0000-0000-000000000020', '00000000-0000-0000-0000-000000000262', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000363', '00000000-0000-0000-0000-000000000020', '00000000-0000-0000-0000-000000000263', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000364', '00000000-0000-0000-0000-000000000020', '00000000-0000-0000-0000-000000000264', 'ACTIVE', NOW(), NULL, NOW(), NULL)
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    updated_at = NOW();
