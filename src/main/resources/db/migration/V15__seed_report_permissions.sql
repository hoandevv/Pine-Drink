-- Pine Drink - Report permissions seed
-- Restrict report export features to ADMIN and MANAGER roles.

-- ============================================================
-- 1. Report Permissions
-- ============================================================
INSERT INTO ia_permission (id, code, name, module, description, status, created_at, created_by, updated_at, updated_by)
VALUES
    ('00000000-0000-0000-0000-0000000002b1', 'REPORT_CREATE', 'Create reports', 'REPORT', 'Create report export jobs', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-0000000002b2', 'REPORT_VIEW', 'View reports', 'REPORT', 'View report export jobs and download generated reports', 'ACTIVE', NOW(), NULL, NOW(), NULL)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    module = VALUES(module),
    description = VALUES(description),
    status = VALUES(status),
    updated_at = NOW();

-- ============================================================
-- 2. Role -> Permission matrix for REPORT module
-- Roles:
-- ADMIN   = 00000000-0000-0000-0000-000000000020
-- MANAGER = 00000000-0000-0000-0000-000000000021
-- ============================================================
INSERT INTO ia_role_permission (id, role_id, permission_id, status, created_at, created_by, updated_at, updated_by)
VALUES
    -- ADMIN: full report access
    ('00000000-0000-0000-0000-0000000004c1', '00000000-0000-0000-0000-000000000020', '00000000-0000-0000-0000-0000000002b1', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-0000000004c2', '00000000-0000-0000-0000-000000000020', '00000000-0000-0000-0000-0000000002b2', 'ACTIVE', NOW(), NULL, NOW(), NULL),

    -- MANAGER: report access for review/demo phase
    ('00000000-0000-0000-0000-0000000004c3', '00000000-0000-0000-0000-000000000021', '00000000-0000-0000-0000-0000000002b1', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-0000000004c4', '00000000-0000-0000-0000-000000000021', '00000000-0000-0000-0000-0000000002b2', 'ACTIVE', NOW(), NULL, NOW(), NULL)
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    updated_at = NOW(),
    updated_by = VALUES(updated_by);
