-- Pine Drink - Seed data (initial data required for application startup)
-- Uses deterministic UUIDs for all seed records.
-- Password hash is BCrypt (cost 10) compatible with Spring Security's BCryptPasswordEncoder.
-- $2b$ prefix is interchangeable with $2a$ in Spring Security 6.x.

-- ============================================================
-- 1. System scope (covers all branches)
-- ============================================================
INSERT INTO ia_scope (id, scope_type, branch_id, status, created_at, created_by)
VALUES ('00000000-0000-0000-0000-000000000010', 'SYSTEM', NULL, 'ACTIVE', NOW(), NULL)
ON DUPLICATE KEY UPDATE scope_type = VALUES(scope_type);

-- ============================================================
-- 2. Default roles
-- ============================================================
INSERT INTO ia_role (id, code, name, description, role_type, status, created_at, created_by, updated_at, updated_by)
VALUES
    ('00000000-0000-0000-0000-000000000020', 'ADMIN', 'Administrator', 'Full system access', 'SYSTEM', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000021', 'MANAGER', 'Manager', 'Branch management access', 'SYSTEM', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000022', 'CUSTOMER', 'Customer', 'Default role for registered customers', 'SYSTEM', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000023', 'DELIVERY', 'Delivery Staff', 'Delivery personnel access', 'SYSTEM', 'ACTIVE', NOW(), NULL, NOW(), NULL)
ON DUPLICATE KEY UPDATE name = VALUES(name), updated_at = NOW();

-- ============================================================
-- 3. Admin account
--    username : admin
--    password : 123456789 (BCrypt hash, cost 10)
-- ============================================================
INSERT INTO ia_account (id, username, password, full_name, email, phone, avatar_url, status, last_login_at, created_at, created_by, updated_at, updated_by)
VALUES ('00000000-0000-0000-0000-000000000030', 'admin', '$2b$10$ryeIkqDKUYWjzeoQzzHgc.QlzkMFWtIr1khgnnpWmelM3HNZh8jui', 'Administrator', 'admin@pine-drink.com', NULL, NULL, 'ACTIVE', NULL, NOW(), NULL, NOW(), NULL)
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), updated_at = NOW();

-- ============================================================
-- 4. Role assignment: admin -> ADMIN role -> SYSTEM scope
-- ============================================================
INSERT INTO ia_account_role_assignment (id, account_id, role_id, scope_id, status, assigned_at, assigned_by, expires_at)
VALUES ('00000000-0000-0000-0000-000000000040', '00000000-0000-0000-0000-000000000030', '00000000-0000-0000-0000-000000000020', '00000000-0000-0000-0000-000000000010', 'ACTIVE', NOW(), NULL, NULL)
ON DUPLICATE KEY UPDATE status = VALUES(status), assigned_at = VALUES(assigned_at);

-- ============================================================
-- 5. Admin customer profile (required by cart/order/address modules)
-- ============================================================
INSERT INTO cu_customer_profile (id, account_id, customer_code, full_name, phone, email, date_of_birth, gender, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000050', '00000000-0000-0000-0000-000000000030', 'KH-ADMIN-000001', 'Administrator', NULL, 'admin@pine-drink.com', NULL, NULL, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), updated_at = NOW();
