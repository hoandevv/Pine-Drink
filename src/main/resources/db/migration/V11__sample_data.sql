-- Pine Drink - Sample data for development/testing
-- Realistic Vietnamese drink shop dataset
-- Deterministic UUIDs, idempotent (ON DUPLICATE KEY UPDATE)

-- ============================================================
-- Brand domain
-- ============================================================
INSERT INTO ce_brand_domain (id, brand_id, domain, public_key, channel, allow_public_register, status, created_at, created_by, updated_at, updated_by)
VALUES ('00000000-0000-0000-0000-000000000100', '00000000-0000-0000-0000-000000000001', 'pine-drink.com', 'PD_WEB_PUBLIC', 'WEB', TRUE, 'ACTIVE', NOW(), NULL, NOW(), NULL)
ON DUPLICATE KEY UPDATE domain = VALUES(domain);

-- ============================================================
-- Branches
-- ============================================================
INSERT INTO ce_branch (id, brand_id, code, name, address, phone, email, latitude, longitude, timezone, supports_pickup, supports_delivery, average_preparation_minutes, status, created_at, created_by, updated_at, updated_by)
VALUES
    ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000001', 'PINE_Q1', 'Pine Drink - Quận 1', '01 Nguyễn Huệ, P. Bến Nghé, Quận 1, TP.HCM', '02838231234', 'q1@pine-drink.com', 10.7721, 106.7043, 'Asia/Ho_Chi_Minh', TRUE, TRUE, 15, 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000001', 'PINE_Q7', 'Pine Drink - Quận 7', '123 Nguyễn Thị Thập, P. Tân Phú, Quận 7, TP.HCM', '02854123456', 'q7@pine-drink.com', 10.7356, 106.7324, 'Asia/Ho_Chi_Minh', TRUE, TRUE, 15, 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000103', '00000000-0000-0000-0000-000000000001', 'PINE_TD', 'Pine Drink - Thủ Đức', '456 Võ Văn Ngân, P. Linh Chiểu, TP. Thủ Đức, TP.HCM', '02838987654', 'thuduc@pine-drink.com', 10.8496, 106.7682, 'Asia/Ho_Chi_Minh', TRUE, FALSE, 20, 'ACTIVE', NOW(), NULL, NOW(), NULL)
ON DUPLICATE KEY UPDATE name = VALUES(name), updated_at = NOW();

-- ============================================================
-- Branch hours (7 days x 3 branches)
-- ============================================================
INSERT INTO ce_branch_hours (id, branch_id, day_of_week, open_time, close_time, is_closed, created_at, updated_at)
VALUES
    -- Quan 1
    ('00000000-0000-0000-0000-000000000111', '00000000-0000-0000-0000-000000000101', 1, '07:00', '22:00', FALSE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000112', '00000000-0000-0000-0000-000000000101', 2, '07:00', '22:00', FALSE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000113', '00000000-0000-0000-0000-000000000101', 3, '07:00', '22:00', FALSE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000114', '00000000-0000-0000-0000-000000000101', 4, '07:00', '22:00', FALSE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000115', '00000000-0000-0000-0000-000000000101', 5, '07:00', '23:00', FALSE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000116', '00000000-0000-0000-0000-000000000101', 6, '07:00', '23:00', FALSE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000117', '00000000-0000-0000-0000-000000000101', 7, '08:00', '22:00', FALSE, NOW(), NOW()),
    -- Quan 7
    ('00000000-0000-0000-0000-000000000121', '00000000-0000-0000-0000-000000000102', 1, '07:30', '21:30', FALSE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000122', '00000000-0000-0000-0000-000000000102', 2, '07:30', '21:30', FALSE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000123', '00000000-0000-0000-0000-000000000102', 3, '07:30', '21:30', FALSE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000124', '00000000-0000-0000-0000-000000000102', 4, '07:30', '21:30', FALSE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000125', '00000000-0000-0000-0000-000000000102', 5, '07:30', '22:30', FALSE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000126', '00000000-0000-0000-0000-000000000102', 6, '07:30', '22:30', FALSE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000127', '00000000-0000-0000-0000-000000000102', 7, '08:00', '21:30', FALSE, NOW(), NOW()),
    -- Thu Duc
    ('00000000-0000-0000-0000-000000000131', '00000000-0000-0000-0000-000000000103', 1, '08:00', '21:00', FALSE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000132', '00000000-0000-0000-0000-000000000103', 2, '08:00', '21:00', FALSE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000133', '00000000-0000-0000-0000-000000000103', 3, '08:00', '21:00', FALSE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000134', '00000000-0000-0000-0000-000000000103', 4, '08:00', '21:00', FALSE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000135', '00000000-0000-0000-0000-000000000103', 5, '08:00', '22:00', FALSE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000136', '00000000-0000-0000-0000-000000000103', 6, '08:00', '22:00', FALSE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000137', '00000000-0000-0000-0000-000000000103', 7, '09:00', '21:00', FALSE, NOW(), NOW())
ON DUPLICATE KEY UPDATE open_time = VALUES(open_time), close_time = VALUES(close_time);

-- ============================================================
-- Pickup time slots (per branch)
-- ============================================================
INSERT INTO ce_pickup_time_slot (id, branch_id, slot_code, start_time, end_time, max_orders, status, created_at, created_by, updated_at, updated_by)
VALUES
    ('00000000-0000-0000-0000-000000000141', '00000000-0000-0000-0000-000000000101', 'MORNING', '08:00', '11:00', 20, 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000142', '00000000-0000-0000-0000-000000000101', 'LUNCH', '11:00', '14:00', 30, 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000143', '00000000-0000-0000-0000-000000000101', 'AFTERNOON', '14:00', '17:00', 25, 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000144', '00000000-0000-0000-0000-000000000101', 'EVENING', '17:00', '21:00', 35, 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000145', '00000000-0000-0000-0000-000000000102', 'MORNING', '08:00', '11:00', 15, 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000146', '00000000-0000-0000-0000-000000000102', 'LUNCH', '11:00', '14:00', 20, 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000147', '00000000-0000-0000-0000-000000000102', 'AFTERNOON', '14:00', '17:00', 20, 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000148', '00000000-0000-0000-0000-000000000102', 'EVENING', '17:00', '21:00', 25, 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000149', '00000000-0000-0000-0000-000000000103', 'MORNING', '08:00', '11:00', 10, 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000150', '00000000-0000-0000-0000-000000000103', 'LUNCH', '11:00', '14:00', 15, 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000151', '00000000-0000-0000-0000-000000000103', 'AFTERNOON', '14:00', '17:00', 15, 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000152', '00000000-0000-0000-0000-000000000103', 'EVENING', '17:00', '20:30', 20, 'ACTIVE', NOW(), NULL, NOW(), NULL)
ON DUPLICATE KEY UPDATE max_orders = VALUES(max_orders);

-- ============================================================
-- Settings
-- ============================================================
INSERT INTO ce_setting (id, brand_id, branch_id, config_key, config_value, data_type, description, is_runtime_editable, status, created_at, created_by, updated_at, updated_by)
VALUES
    ('00000000-0000-0000-0000-000000000161', '00000000-0000-0000-0000-000000000001', NULL, 'order_cancel_timeout_minutes', '15', 'INT', 'Thoi gian cho phep huy don (phut)', TRUE, 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000162', '00000000-0000-0000-0000-000000000001', NULL, 'max_order_items', '20', 'INT', 'So luong toi da mon trong 1 don', TRUE, 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000163', '00000000-0000-0000-0000-000000000001', NULL, 'default_timezone', 'Asia/Ho_Chi_Minh', 'STRING', 'Mui gio mac dinh', FALSE, 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000164', '00000000-0000-0000-0000-000000000001', NULL, 'otp_expiration_minutes', '5', 'INT', 'Thoi gian hieu luc OTP (phut)', TRUE, 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000165', '00000000-0000-0000-0000-000000000001', NULL, 'max_login_attempts', '5', 'INT', 'So lan dang nhap sai toi da', TRUE, 'ACTIVE', NOW(), NULL, NOW(), NULL)
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

-- ============================================================
-- Permissions
-- ============================================================
INSERT INTO ia_permission (id, code, name, module, description, status, created_at, created_by, updated_at, updated_by)
VALUES
    ('00000000-0000-0000-0000-000000000201', 'BRAND_VIEW', 'Xem thuong hieu', 'BRAND', 'Xem thong tin thuong hieu', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000202', 'BRAND_UPDATE', 'Cap nhat thuong hieu', 'BRAND', 'Cap nhat thong tin thuong hieu', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000203', 'BRANCH_VIEW', 'Xem chi nhanh', 'BRANCH', 'Xem danh sach chi nhanh', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000204', 'BRANCH_CREATE', 'Tao chi nhanh', 'BRANCH', 'Tao chi nhanh moi', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000205', 'BRANCH_UPDATE', 'Cap nhat chi nhanh', 'BRANCH', 'Cap nhat thong tin chi nhanh', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000206', 'ACCOUNT_VIEW', 'Xem tai khoan', 'ACCOUNT', 'Xem danh sach tai khoan', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000207', 'ACCOUNT_CREATE', 'Tao tai khoan', 'ACCOUNT', 'Tao tai khoan moi', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000208', 'ACCOUNT_UPDATE', 'Cap nhat tai khoan', 'ACCOUNT', 'Cap nhat thong tin tai khoan', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000209', 'ACCOUNT_DELETE', 'Xoa tai khoan', 'ACCOUNT', 'Xoa tai khoan', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000210', 'ROLE_VIEW', 'Xem vai tro', 'ROLE', 'Xem danh sach vai tro', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000211', 'ROLE_ASSIGN', 'Gan vai tro', 'ROLE', 'Gan vai tro cho tai khoan', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000212', 'CUSTOMER_VIEW', 'Xem khach hang', 'CUSTOMER', 'Xem danh sach khach hang', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000213', 'CUSTOMER_UPDATE', 'Cap nhat khach hang', 'CUSTOMER', 'Cap nhat thong tin khach hang', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000214', 'CATEGORY_VIEW', 'Xem danh muc', 'CATEGORY', 'Xem danh muc san pham', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000215', 'CATEGORY_CREATE', 'Tao danh muc', 'CATEGORY', 'Tao danh muc san pham moi', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000216', 'CATEGORY_UPDATE', 'Cap nhat danh muc', 'CATEGORY', 'Cap nhat danh muc san pham', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000217', 'PRODUCT_VIEW', 'Xem san pham', 'PRODUCT', 'Xem danh sach san pham', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000218', 'PRODUCT_CREATE', 'Tao san pham', 'PRODUCT', 'Tao san pham moi', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000219', 'PRODUCT_UPDATE', 'Cap nhat san pham', 'PRODUCT', 'Cap nhat thong tin san pham', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000220', 'PRODUCT_DELETE', 'Xoa san pham', 'PRODUCT', 'Xoa san pham', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000221', 'ORDER_VIEW', 'Xem don hang', 'ORDER', 'Xem danh sach don hang', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000222', 'ORDER_UPDATE_STATUS', 'Cap nhat trang thai don', 'ORDER', 'Cap nhat trang thai don hang', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000223', 'ORDER_CANCEL', 'Huy don hang', 'ORDER', 'Huy don hang', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000224', 'VOUCHER_VIEW', 'Xem voucher', 'VOUCHER', 'Xem danh sach voucher', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000225', 'VOUCHER_CREATE', 'Tao voucher', 'VOUCHER', 'Tao voucher moi', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000226', 'VOUCHER_UPDATE', 'Cap nhat voucher', 'VOUCHER', 'Cap nhat voucher', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000227', 'INVENTORY_VIEW', 'Xem ton kho', 'INVENTORY', 'Xem ton kho nguyen lieu', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000228', 'INVENTORY_IMPORT', 'Nhap kho', 'INVENTORY', 'Nhap kho nguyen lieu', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000229', 'REPORT_VIEW', 'Xem bao cao', 'REPORT', 'Xem cac bao cao thong ke', 'ACTIVE', NOW(), NULL, NOW(), NULL),
    ('00000000-0000-0000-0000-000000000230', 'REPORT_EXPORT', 'Xuat bao cao', 'REPORT', 'Xuat bao cao (Excel/PDF)', 'ACTIVE', NOW(), NULL, NOW(), NULL)
ON DUPLICATE KEY UPDATE name = VALUES(name);
