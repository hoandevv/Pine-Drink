-- ============================================================
-- V1__create_ce_brand_branch_schema.sql
-- ============================================================

-- Pine Drink - Company / Brand / Branch schema

CREATE TABLE ce_brand (
    id CHAR(36) NOT NULL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    legal_name VARCHAR(200) NULL,
    tax_code VARCHAR(50) NULL,
    address VARCHAR(255) NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(150) NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_ce_brand_code (code),
    INDEX idx_ce_brand_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ce_brand_domain (
    id CHAR(36) NOT NULL PRIMARY KEY,
    brand_id CHAR(36) NOT NULL,
    domain VARCHAR(255) NOT NULL,
    public_key VARCHAR(100) NOT NULL,
    channel VARCHAR(30) NOT NULL DEFAULT 'WEB',
    allow_public_register BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_ce_brand_domain_domain_public_key (domain, public_key),
    UNIQUE KEY uk_ce_brand_domain_public_key (public_key),
    CONSTRAINT fk_ce_brand_domain_brand FOREIGN KEY (brand_id) REFERENCES ce_brand(id) ON DELETE RESTRICT,
    INDEX idx_ce_brand_domain_brand_status (brand_id, status),
    INDEX idx_ce_brand_domain_domain_status (domain, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ce_branch (
    id CHAR(36) NOT NULL PRIMARY KEY,
    brand_id CHAR(36) NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    address VARCHAR(255) NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(150) NULL,
    latitude DECIMAL(10, 7) NULL,
    longitude DECIMAL(10, 7) NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    supports_pickup BOOLEAN NOT NULL DEFAULT TRUE,
    supports_delivery BOOLEAN NOT NULL DEFAULT FALSE,
    average_preparation_minutes INT NOT NULL DEFAULT 15,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_ce_branch_brand_code (brand_id, code),
    CONSTRAINT fk_ce_branch_brand FOREIGN KEY (brand_id) REFERENCES ce_brand(id) ON DELETE CASCADE,
    INDEX idx_ce_branch_brand_status (brand_id, status),
    INDEX idx_ce_branch_status (status),
    INDEX idx_ce_branch_location (latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ce_branch_hours (
    id CHAR(36) NOT NULL PRIMARY KEY,
    branch_id CHAR(36) NOT NULL,
    day_of_week TINYINT NOT NULL,
    open_time TIME NOT NULL,
    close_time TIME NOT NULL,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    CONSTRAINT fk_ce_branch_hours_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id) ON DELETE CASCADE,
    UNIQUE KEY uk_ce_branch_hours_branch_day (branch_id, day_of_week),
    INDEX idx_ce_branch_hours_branch (branch_id),
    CHECK (day_of_week BETWEEN 1 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ce_pickup_time_slot (
    id CHAR(36) NOT NULL PRIMARY KEY,
    branch_id CHAR(36) NOT NULL,
    slot_code VARCHAR(50) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    max_orders INT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_ce_pickup_slot_branch_code (branch_id, slot_code),
    CONSTRAINT fk_ce_pickup_slot_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id) ON DELETE CASCADE,
    INDEX idx_ce_pickup_slot_branch_status (branch_id, status),
    INDEX idx_ce_pickup_slot_time (branch_id, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ce_setting (
    id CHAR(36) NOT NULL PRIMARY KEY,
    brand_id CHAR(36) NULL,
    branch_id CHAR(36) NULL,
    config_key VARCHAR(100) NOT NULL,
    config_value VARCHAR(500) NOT NULL,
    data_type VARCHAR(20) NOT NULL,
    description VARCHAR(255) NULL,
    is_runtime_editable BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_ce_setting_scope_key (brand_id, branch_id, config_key),
    CONSTRAINT fk_ce_setting_brand FOREIGN KEY (brand_id) REFERENCES ce_brand(id) ON DELETE CASCADE,
    CONSTRAINT fk_ce_setting_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id) ON DELETE CASCADE,
    INDEX idx_ce_setting_key (config_key),
    INDEX idx_ce_setting_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- V2__create_ia_identity_access_schema.sql
-- ============================================================

-- Pine Drink - Identity & Access schema

CREATE TABLE ia_account (
    id CHAR(36) NOT NULL PRIMARY KEY,
    brand_id CHAR(36) NULL,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NULL,
    phone VARCHAR(20) NULL,
    avatar_url VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_ia_account_username (username),
    UNIQUE KEY uk_ia_account_email (email),
    UNIQUE KEY uk_ia_account_phone (phone),
    CONSTRAINT fk_ia_account_brand FOREIGN KEY (brand_id) REFERENCES ce_brand(id) ON DELETE SET NULL,
    INDEX idx_ia_account_brand_status (brand_id, status),
    INDEX idx_ia_account_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ia_role (
    id CHAR(36) NOT NULL PRIMARY KEY,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(255) NULL,
    role_type VARCHAR(30) NOT NULL DEFAULT 'SYSTEM',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_ia_role_code (code),
    INDEX idx_ia_role_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ia_permission (
    id CHAR(36) NOT NULL PRIMARY KEY,
    code VARCHAR(120) NOT NULL,
    name VARCHAR(150) NOT NULL,
    module VARCHAR(80) NOT NULL,
    description VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_ia_permission_code (code),
    INDEX idx_ia_permission_module_status (module, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ia_role_permission (
    id CHAR(36) NOT NULL PRIMARY KEY,
    role_id CHAR(36) NOT NULL,
    permission_id CHAR(36) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    UNIQUE KEY uk_ia_role_permission (role_id, permission_id),
    CONSTRAINT fk_ia_role_permission_role FOREIGN KEY (role_id) REFERENCES ia_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_ia_role_permission_permission FOREIGN KEY (permission_id) REFERENCES ia_permission(id) ON DELETE CASCADE,
    INDEX idx_ia_role_permission_role (role_id),
    INDEX idx_ia_role_permission_permission (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ia_scope (
    id CHAR(36) NOT NULL PRIMARY KEY,
    scope_type VARCHAR(30) NOT NULL,
    brand_id CHAR(36) NULL,
    branch_id CHAR(36) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    UNIQUE KEY uk_ia_scope_unique (scope_type, brand_id, branch_id),
    CONSTRAINT fk_ia_scope_brand FOREIGN KEY (brand_id) REFERENCES ce_brand(id) ON DELETE CASCADE,
    CONSTRAINT fk_ia_scope_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id) ON DELETE CASCADE,
    INDEX idx_ia_scope_type_status (scope_type, status),
    INDEX idx_ia_scope_brand_branch (brand_id, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ia_account_role_assignment (
    id CHAR(36) NOT NULL PRIMARY KEY,
    account_id CHAR(36) NOT NULL,
    role_id CHAR(36) NOT NULL,
    scope_id CHAR(36) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by CHAR(36) NULL,
    expires_at DATETIME NULL,
    UNIQUE KEY uk_ia_account_role_scope (account_id, role_id, scope_id),
    CONSTRAINT fk_ia_ara_account FOREIGN KEY (account_id) REFERENCES ia_account(id) ON DELETE CASCADE,
    CONSTRAINT fk_ia_ara_role FOREIGN KEY (role_id) REFERENCES ia_role(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ia_ara_scope FOREIGN KEY (scope_id) REFERENCES ia_scope(id) ON DELETE RESTRICT,
    INDEX idx_ia_ara_account_status (account_id, status),
    INDEX idx_ia_ara_role_scope (role_id, scope_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ia_audit_log (
    id CHAR(36) NOT NULL PRIMARY KEY,
    actor_account_id CHAR(36) NULL,
    action VARCHAR(100) NOT NULL,
    module VARCHAR(80) NOT NULL,
    target_type VARCHAR(80) NULL,
    target_id CHAR(36) NULL,
    brand_id CHAR(36) NULL,
    branch_id CHAR(36) NULL,
    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(500) NULL,
    before_data JSON NULL,
    after_data JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ia_audit_actor FOREIGN KEY (actor_account_id) REFERENCES ia_account(id) ON DELETE SET NULL,
    INDEX idx_ia_audit_actor_created (actor_account_id, created_at),
    INDEX idx_ia_audit_target (target_type, target_id),
    INDEX idx_ia_audit_brand_branch_created (brand_id, branch_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ia_refresh_token (
    id CHAR(36) NOT NULL PRIMARY KEY,
    account_id CHAR(36) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    device_info VARCHAR(255) NULL,
    ip_address VARCHAR(64) NULL,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ia_refresh_token_account FOREIGN KEY (account_id) REFERENCES ia_account(id) ON DELETE CASCADE,
    UNIQUE KEY uk_ia_refresh_token_hash (token_hash),
    INDEX idx_ia_refresh_token_account_expires (account_id, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- V3__create_cu_customer_schema.sql
-- ============================================================

-- Pine Drink - Customer schema

CREATE TABLE cu_customer_profile (
    id CHAR(36) NOT NULL PRIMARY KEY,
    account_id CHAR(36) NOT NULL,
    customer_code VARCHAR(50) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(150) NULL,
    date_of_birth DATE NULL,
    gender VARCHAR(20) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_cu_customer_account (account_id),
    UNIQUE KEY uk_cu_customer_code (customer_code),
    CONSTRAINT fk_cu_customer_account FOREIGN KEY (account_id) REFERENCES ia_account(id) ON DELETE CASCADE,
    INDEX idx_cu_customer_phone (phone),
    INDEX idx_cu_customer_email (email),
    INDEX idx_cu_customer_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cu_customer_address (
    id CHAR(36) NOT NULL PRIMARY KEY,
    customer_id CHAR(36) NOT NULL,
    receiver_name VARCHAR(150) NOT NULL,
    receiver_phone VARCHAR(20) NOT NULL,
    address_line VARCHAR(255) NOT NULL,
    ward VARCHAR(100) NULL,
    district VARCHAR(100) NULL,
    city VARCHAR(100) NULL,
    latitude DECIMAL(10, 7) NULL,
    longitude DECIMAL(10, 7) NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    CONSTRAINT fk_cu_address_customer FOREIGN KEY (customer_id) REFERENCES cu_customer_profile(id) ON DELETE CASCADE,
    INDEX idx_cu_address_customer_status (customer_id, status),
    INDEX idx_cu_address_default (customer_id, is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cu_loyalty_account (
    id CHAR(36) NOT NULL PRIMARY KEY,
    customer_id CHAR(36) NOT NULL,
    tier VARCHAR(30) NOT NULL DEFAULT 'SILVER',
    points_balance INT NOT NULL DEFAULT 0,
    lifetime_points INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cu_loyalty_customer (customer_id),
    CONSTRAINT fk_cu_loyalty_customer FOREIGN KEY (customer_id) REFERENCES cu_customer_profile(id) ON DELETE CASCADE,
    INDEX idx_cu_loyalty_tier (tier),
    CHECK (points_balance >= 0),
    CHECK (lifetime_points >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cu_loyalty_point_history (
    id CHAR(36) NOT NULL PRIMARY KEY,
    loyalty_account_id CHAR(36) NOT NULL,
    order_id CHAR(36) NULL,
    transaction_type VARCHAR(30) NOT NULL,
    points INT NOT NULL,
    balance_after INT NOT NULL,
    reason VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    CONSTRAINT fk_cu_point_history_loyalty FOREIGN KEY (loyalty_account_id) REFERENCES cu_loyalty_account(id) ON DELETE CASCADE,
    INDEX idx_cu_point_history_loyalty_created (loyalty_account_id, created_at),
    INDEX idx_cu_point_history_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- V4__create_pr_menu_schema.sql
-- ============================================================

-- Pine Drink - Product Catalog and Menu schema

CREATE TABLE pr_category (
    id CHAR(36) NOT NULL PRIMARY KEY,
    brand_id CHAR(36) NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(255) NULL,
    image_url VARCHAR(500) NULL,
    display_order INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_pr_category_brand_code (brand_id, code),
    CONSTRAINT fk_pr_category_brand FOREIGN KEY (brand_id) REFERENCES ce_brand(id) ON DELETE CASCADE,
    INDEX idx_pr_category_brand_status_order (brand_id, status, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pr_product (
    id CHAR(36) NOT NULL PRIMARY KEY,
    brand_id CHAR(36) NOT NULL,
    category_id CHAR(36) NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    image_url VARCHAR(500) NULL,
    base_price DECIMAL(12,2) NOT NULL,
    preparation_minutes INT NOT NULL DEFAULT 10,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    is_best_seller BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_pr_product_brand_code (brand_id, code),
    CONSTRAINT fk_pr_product_brand FOREIGN KEY (brand_id) REFERENCES ce_brand(id) ON DELETE CASCADE,
    CONSTRAINT fk_pr_product_category FOREIGN KEY (category_id) REFERENCES pr_category(id) ON DELETE RESTRICT,
    INDEX idx_pr_product_category_status (category_id, status),
    INDEX idx_pr_product_brand_status (brand_id, status),
    INDEX idx_pr_product_featured (brand_id, is_featured, status),
    CHECK (base_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pr_product_variant (
    id CHAR(36) NOT NULL PRIMARY KEY,
    product_id CHAR(36) NOT NULL,
    variant_code VARCHAR(50) NOT NULL,
    variant_name VARCHAR(100) NOT NULL,
    size_label VARCHAR(30) NOT NULL,
    price_delta DECIMAL(12,2) NOT NULL DEFAULT 0,
    display_order INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_pr_variant_product_code (product_id, variant_code),
    CONSTRAINT fk_pr_variant_product FOREIGN KEY (product_id) REFERENCES pr_product(id) ON DELETE CASCADE,
    INDEX idx_pr_variant_product_status_order (product_id, status, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pr_topping (
    id CHAR(36) NOT NULL PRIMARY KEY,
    brand_id CHAR(36) NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    image_url VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_pr_topping_brand_code (brand_id, code),
    CONSTRAINT fk_pr_topping_brand FOREIGN KEY (brand_id) REFERENCES ce_brand(id) ON DELETE CASCADE,
    INDEX idx_pr_topping_brand_status (brand_id, status),
    CHECK (price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pr_product_topping (
    id CHAR(36) NOT NULL PRIMARY KEY,
    product_id CHAR(36) NOT NULL,
    topping_id CHAR(36) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    max_quantity INT NOT NULL DEFAULT 3,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    UNIQUE KEY uk_pr_product_topping (product_id, topping_id),
    CONSTRAINT fk_pr_product_topping_product FOREIGN KEY (product_id) REFERENCES pr_product(id) ON DELETE CASCADE,
    CONSTRAINT fk_pr_product_topping_topping FOREIGN KEY (topping_id) REFERENCES pr_topping(id) ON DELETE RESTRICT,
    INDEX idx_pr_product_topping_product_status (product_id, status),
    INDEX idx_pr_product_topping_topping (topping_id),
    CHECK (max_quantity >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE mn_branch_product_availability (
    id CHAR(36) NOT NULL PRIMARY KEY,
    branch_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    sale_price DECIMAL(12,2) NULL,
    sold_out_reason VARCHAR(255) NULL,
    available_from DATETIME NULL,
    available_to DATETIME NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_mn_branch_product (branch_id, product_id),
    CONSTRAINT fk_mn_branch_product_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id) ON DELETE CASCADE,
    CONSTRAINT fk_mn_branch_product_product FOREIGN KEY (product_id) REFERENCES pr_product(id) ON DELETE CASCADE,
    INDEX idx_mn_branch_product_branch_available (branch_id, is_available),
    INDEX idx_mn_branch_product_product (product_id),
    CHECK (sale_price IS NULL OR sale_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE mn_branch_topping_availability (
    id CHAR(36) NOT NULL PRIMARY KEY,
    branch_id CHAR(36) NOT NULL,
    topping_id CHAR(36) NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    sold_out_reason VARCHAR(255) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_mn_branch_topping (branch_id, topping_id),
    CONSTRAINT fk_mn_branch_topping_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id) ON DELETE CASCADE,
    CONSTRAINT fk_mn_branch_topping_topping FOREIGN KEY (topping_id) REFERENCES pr_topping(id) ON DELETE CASCADE,
    INDEX idx_mn_branch_topping_branch_available (branch_id, is_available)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- V5__create_ca_cart_schema.sql
-- ============================================================

-- Pine Drink - Cart schema

CREATE TABLE ca_cart (
    id CHAR(36) NOT NULL PRIMARY KEY,
    customer_id CHAR(36) NULL,
    branch_id CHAR(36) NOT NULL,
    session_id VARCHAR(120) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ca_cart_customer FOREIGN KEY (customer_id) REFERENCES cu_customer_profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_ca_cart_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id) ON DELETE RESTRICT,
    INDEX idx_ca_cart_customer_status (customer_id, status),
    INDEX idx_ca_cart_session_status (session_id, status),
    INDEX idx_ca_cart_branch (branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ca_cart_item (
    id CHAR(36) NOT NULL PRIMARY KEY,
    cart_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    variant_id CHAR(36) NULL,
    quantity INT NOT NULL,
    sugar_level VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
    ice_level VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
    note VARCHAR(255) NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    total_price DECIMAL(12,2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ca_item_cart FOREIGN KEY (cart_id) REFERENCES ca_cart(id) ON DELETE CASCADE,
    CONSTRAINT fk_ca_item_product FOREIGN KEY (product_id) REFERENCES pr_product(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ca_item_variant FOREIGN KEY (variant_id) REFERENCES pr_product_variant(id) ON DELETE SET NULL,
    INDEX idx_ca_item_cart (cart_id),
    INDEX idx_ca_item_product (product_id),
    CHECK (quantity > 0),
    CHECK (unit_price >= 0),
    CHECK (total_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ca_cart_item_topping (
    id CHAR(36) NOT NULL PRIMARY KEY,
    cart_item_id CHAR(36) NOT NULL,
    topping_id CHAR(36) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(12,2) NOT NULL,
    total_price DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_ca_item_topping_item FOREIGN KEY (cart_item_id) REFERENCES ca_cart_item(id) ON DELETE CASCADE,
    CONSTRAINT fk_ca_item_topping_topping FOREIGN KEY (topping_id) REFERENCES pr_topping(id) ON DELETE RESTRICT,
    INDEX idx_ca_item_topping_item (cart_item_id),
    INDEX idx_ca_item_topping_topping (topping_id),
    CHECK (quantity > 0),
    CHECK (unit_price >= 0),
    CHECK (total_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- V6__create_od_order_schema.sql
-- ============================================================

-- Pine Drink - Order schema

CREATE TABLE od_order (
    id CHAR(36) NOT NULL PRIMARY KEY,
    order_code VARCHAR(50) NOT NULL,
    brand_id CHAR(36) NOT NULL,
    branch_id CHAR(36) NOT NULL,
    customer_id CHAR(36) NULL,
    customer_name VARCHAR(150) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    customer_email VARCHAR(150) NULL,
    order_type VARCHAR(30) NOT NULL DEFAULT 'PICKUP',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_status VARCHAR(30) NOT NULL DEFAULT 'UNPAID',
    subtotal_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    delivery_fee DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    pickup_time DATETIME NULL,
    delivery_address VARCHAR(255) NULL,
    note VARCHAR(500) NULL,
    confirmed_at DATETIME NULL,
    prepared_at DATETIME NULL,
    ready_at DATETIME NULL,
    completed_at DATETIME NULL,
    cancelled_at DATETIME NULL,
    cancel_reason VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_od_order_code (order_code),
    CONSTRAINT fk_od_order_brand FOREIGN KEY (brand_id) REFERENCES ce_brand(id) ON DELETE RESTRICT,
    CONSTRAINT fk_od_order_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id) ON DELETE RESTRICT,
    CONSTRAINT fk_od_order_customer FOREIGN KEY (customer_id) REFERENCES cu_customer_profile(id) ON DELETE SET NULL,
    INDEX idx_od_order_customer_created (customer_id, created_at),
    INDEX idx_od_order_branch_status_created (branch_id, status, created_at),
    INDEX idx_od_order_brand_created (brand_id, created_at),
    INDEX idx_od_order_payment_status (payment_status),
    CHECK (subtotal_amount >= 0),
    CHECK (discount_amount >= 0),
    CHECK (delivery_fee >= 0),
    CHECK (total_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE od_order_item (
    id CHAR(36) NOT NULL PRIMARY KEY,
    order_id CHAR(36) NOT NULL,
    product_id CHAR(36) NULL,
    variant_id CHAR(36) NULL,
    product_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(150) NOT NULL,
    variant_name VARCHAR(100) NULL,
    quantity INT NOT NULL,
    sugar_level VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
    ice_level VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
    note VARCHAR(255) NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    total_price DECIMAL(12,2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_od_item_order FOREIGN KEY (order_id) REFERENCES od_order(id) ON DELETE CASCADE,
    CONSTRAINT fk_od_item_product FOREIGN KEY (product_id) REFERENCES pr_product(id) ON DELETE SET NULL,
    CONSTRAINT fk_od_item_variant FOREIGN KEY (variant_id) REFERENCES pr_product_variant(id) ON DELETE SET NULL,
    INDEX idx_od_item_order (order_id),
    INDEX idx_od_item_product (product_id),
    CHECK (quantity > 0),
    CHECK (unit_price >= 0),
    CHECK (total_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE od_order_item_topping (
    id CHAR(36) NOT NULL PRIMARY KEY,
    order_item_id CHAR(36) NOT NULL,
    topping_id CHAR(36) NULL,
    topping_code VARCHAR(50) NOT NULL,
    topping_name VARCHAR(150) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(12,2) NOT NULL,
    total_price DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_od_item_topping_item FOREIGN KEY (order_item_id) REFERENCES od_order_item(id) ON DELETE CASCADE,
    CONSTRAINT fk_od_item_topping_topping FOREIGN KEY (topping_id) REFERENCES pr_topping(id) ON DELETE SET NULL,
    INDEX idx_od_item_topping_item (order_item_id),
    CHECK (quantity > 0),
    CHECK (unit_price >= 0),
    CHECK (total_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE od_order_status_history (
    id CHAR(36) NOT NULL PRIMARY KEY,
    order_id CHAR(36) NOT NULL,
    old_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NOT NULL,
    reason VARCHAR(255) NULL,
    changed_by CHAR(36) NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_od_status_history_order FOREIGN KEY (order_id) REFERENCES od_order(id) ON DELETE CASCADE,
    CONSTRAINT fk_od_status_history_actor FOREIGN KEY (changed_by) REFERENCES ia_account(id) ON DELETE SET NULL,
    INDEX idx_od_status_history_order_changed (order_id, changed_at),
    INDEX idx_od_status_history_status (new_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- V7__create_vc_payment_schema.sql
-- ============================================================

-- Pine Drink - Voucher and Payment schema

CREATE TABLE vc_voucher (
    id CHAR(36) NOT NULL PRIMARY KEY,
    brand_id CHAR(36) NOT NULL,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    discount_type VARCHAR(30) NOT NULL,
    discount_value DECIMAL(12,2) NOT NULL,
    max_discount_amount DECIMAL(12,2) NULL,
    min_order_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    usage_limit INT NULL,
    used_count INT NOT NULL DEFAULT 0,
    usage_limit_per_customer INT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_vc_voucher_brand_code (brand_id, code),
    CONSTRAINT fk_vc_voucher_brand FOREIGN KEY (brand_id) REFERENCES ce_brand(id) ON DELETE CASCADE,
    INDEX idx_vc_voucher_brand_status_time (brand_id, status, start_at, end_at),
    CHECK (discount_value >= 0),
    CHECK (max_discount_amount IS NULL OR max_discount_amount >= 0),
    CHECK (min_order_amount >= 0),
    CHECK (used_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vc_voucher_branch (
    id CHAR(36) NOT NULL PRIMARY KEY,
    voucher_id CHAR(36) NOT NULL,
    branch_id CHAR(36) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_vc_voucher_branch (voucher_id, branch_id),
    CONSTRAINT fk_vc_voucher_branch_voucher FOREIGN KEY (voucher_id) REFERENCES vc_voucher(id) ON DELETE CASCADE,
    CONSTRAINT fk_vc_voucher_branch_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vc_voucher_usage (
    id CHAR(36) NOT NULL PRIMARY KEY,
    voucher_id CHAR(36) NOT NULL,
    order_id CHAR(36) NOT NULL,
    customer_id CHAR(36) NULL,
    discount_amount DECIMAL(12,2) NOT NULL,
    used_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_vc_usage_voucher_order (voucher_id, order_id),
    CONSTRAINT fk_vc_usage_voucher FOREIGN KEY (voucher_id) REFERENCES vc_voucher(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vc_usage_order FOREIGN KEY (order_id) REFERENCES od_order(id) ON DELETE CASCADE,
    CONSTRAINT fk_vc_usage_customer FOREIGN KEY (customer_id) REFERENCES cu_customer_profile(id) ON DELETE SET NULL,
    INDEX idx_vc_usage_customer_voucher (customer_id, voucher_id),
    INDEX idx_vc_usage_voucher_used (voucher_id, used_at),
    CHECK (discount_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE py_payment_intent (
    id CHAR(36) NOT NULL PRIMARY KEY,
    order_id CHAR(36) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    request_payload JSON NULL,
    response_payload JSON NULL,
    expires_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_py_intent_order_provider (order_id, provider),
    CONSTRAINT fk_py_intent_order FOREIGN KEY (order_id) REFERENCES od_order(id) ON DELETE CASCADE,
    INDEX idx_py_intent_status_created (status, created_at),
    CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE py_transaction (
    id CHAR(36) NOT NULL PRIMARY KEY,
    order_id CHAR(36) NOT NULL,
    payment_intent_id CHAR(36) NULL,
    transaction_code VARCHAR(100) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    paid_at DATETIME NULL,
    failed_reason VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_py_transaction_code (transaction_code),
    CONSTRAINT fk_py_transaction_order FOREIGN KEY (order_id) REFERENCES od_order(id) ON DELETE RESTRICT,
    CONSTRAINT fk_py_transaction_intent FOREIGN KEY (payment_intent_id) REFERENCES py_payment_intent(id) ON DELETE SET NULL,
    INDEX idx_py_transaction_order (order_id),
    INDEX idx_py_transaction_status_created (status, created_at),
    CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE py_callback_log (
    id CHAR(36) NOT NULL PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    transaction_code VARCHAR(100) NULL,
    request_id VARCHAR(120) NULL,
    raw_payload JSON NOT NULL,
    signature_valid BOOLEAN NOT NULL DEFAULT FALSE,
    processing_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    error_message VARCHAR(500) NULL,
    received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME NULL,
    UNIQUE KEY uk_py_callback_provider_request (provider, request_id),
    INDEX idx_py_callback_transaction (transaction_code),
    INDEX idx_py_callback_status_received (processing_status, received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE py_refund (
    id CHAR(36) NOT NULL PRIMARY KEY,
    transaction_id CHAR(36) NOT NULL,
    refund_code VARCHAR(100) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    reason VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    requested_by CHAR(36) NULL,
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    UNIQUE KEY uk_py_refund_code (refund_code),
    CONSTRAINT fk_py_refund_transaction FOREIGN KEY (transaction_id) REFERENCES py_transaction(id) ON DELETE RESTRICT,
    CONSTRAINT fk_py_refund_requested_by FOREIGN KEY (requested_by) REFERENCES ia_account(id) ON DELETE SET NULL,
    INDEX idx_py_refund_transaction (transaction_id),
    INDEX idx_py_refund_status (status),
    CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- V8__create_nt_rp_pf_schema.sql
-- ============================================================

-- Pine Drink - Notification, Report and Platform schema

CREATE TABLE nt_notification (
    id CHAR(36) NOT NULL PRIMARY KEY,
    recipient_account_id CHAR(36) NULL,
    brand_id CHAR(36) NULL,
    branch_id CHAR(36) NULL,
    notification_type VARCHAR(80) NOT NULL,
    title VARCHAR(180) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    channel VARCHAR(30) NOT NULL DEFAULT 'IN_APP',
    status VARCHAR(30) NOT NULL DEFAULT 'UNREAD',
    reference_type VARCHAR(80) NULL,
    reference_id CHAR(36) NULL,
    metadata JSON NULL,
    sent_at DATETIME NULL,
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_nt_notification_account FOREIGN KEY (recipient_account_id) REFERENCES ia_account(id) ON DELETE CASCADE,
    CONSTRAINT fk_nt_notification_brand FOREIGN KEY (brand_id) REFERENCES ce_brand(id) ON DELETE CASCADE,
    CONSTRAINT fk_nt_notification_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id) ON DELETE CASCADE,
    INDEX idx_nt_notification_recipient_status_created (recipient_account_id, status, created_at),
    INDEX idx_nt_notification_reference (reference_type, reference_id),
    INDEX idx_nt_notification_branch_created (branch_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE nt_template (
    id CHAR(36) NOT NULL PRIMARY KEY,
    brand_id CHAR(36) NULL,
    template_code VARCHAR(100) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    subject VARCHAR(180) NULL,
    body TEXT NOT NULL,
    variables JSON NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_nt_template_brand_code_channel (brand_id, template_code, channel),
    CONSTRAINT fk_nt_template_brand FOREIGN KEY (brand_id) REFERENCES ce_brand(id) ON DELETE CASCADE,
    INDEX idx_nt_template_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rp_export_request (
    id CHAR(36) NOT NULL PRIMARY KEY,
    brand_id CHAR(36) NOT NULL,
    branch_id CHAR(36) NULL,
    requested_by CHAR(36) NOT NULL,
    report_type VARCHAR(80) NOT NULL,
    file_format VARCHAR(20) NOT NULL DEFAULT 'XLSX',
    filters JSON NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    file_url VARCHAR(500) NULL,
    error_message VARCHAR(500) NULL,
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    CONSTRAINT fk_rp_export_brand FOREIGN KEY (brand_id) REFERENCES ce_brand(id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_export_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id) ON DELETE SET NULL,
    CONSTRAINT fk_rp_export_requested_by FOREIGN KEY (requested_by) REFERENCES ia_account(id) ON DELETE RESTRICT,
    INDEX idx_rp_export_requested_status (requested_by, status, requested_at),
    INDEX idx_rp_export_brand_type (brand_id, report_type, requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pf_outbox_event (
    id CHAR(36) NOT NULL PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id CHAR(36) NOT NULL,
    brand_id CHAR(36) NULL,
    branch_id CHAR(36) NULL,
    routing_key VARCHAR(150) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 5,
    next_retry_at DATETIME NULL,
    locked_by VARCHAR(100) NULL,
    locked_at DATETIME NULL,
    published_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error VARCHAR(1000) NULL,
    INDEX idx_pf_outbox_status_retry (status, next_retry_at, created_at),
    INDEX idx_pf_outbox_aggregate (aggregate_type, aggregate_id),
    INDEX idx_pf_outbox_branch_created (branch_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pf_idempotency_key (
    id CHAR(36) NOT NULL PRIMARY KEY,
    idempotency_key VARCHAR(150) NOT NULL,
    request_hash VARCHAR(255) NOT NULL,
    response_body JSON NULL,
    http_status INT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PROCESSING',
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pf_idempotency_key (idempotency_key),
    INDEX idx_pf_idempotency_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- V9__create_iv_inventory_schema.sql
-- ============================================================

-- Pine Drink - Inventory schema
-- Optional phase. Useful when tracking ingredients, recipes and stock movement.

CREATE TABLE iv_ingredient (
    id CHAR(36) NOT NULL PRIMARY KEY,
    brand_id CHAR(36) NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    min_stock_quantity DECIMAL(12,3) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_iv_ingredient_brand_code (brand_id, code),
    CONSTRAINT fk_iv_ingredient_brand FOREIGN KEY (brand_id) REFERENCES ce_brand(id) ON DELETE CASCADE,
    INDEX idx_iv_ingredient_brand_status (brand_id, status),
    CHECK (min_stock_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE iv_recipe (
    id CHAR(36) NOT NULL PRIMARY KEY,
    product_id CHAR(36) NOT NULL,
    variant_id CHAR(36) NULL,
    name VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    CONSTRAINT fk_iv_recipe_product FOREIGN KEY (product_id) REFERENCES pr_product(id) ON DELETE CASCADE,
    CONSTRAINT fk_iv_recipe_variant FOREIGN KEY (variant_id) REFERENCES pr_product_variant(id) ON DELETE CASCADE,
    INDEX idx_iv_recipe_product_status (product_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE iv_recipe_item (
    id CHAR(36) NOT NULL PRIMARY KEY,
    recipe_id CHAR(36) NOT NULL,
    ingredient_id CHAR(36) NOT NULL,
    quantity DECIMAL(12,3) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_iv_recipe_ingredient (recipe_id, ingredient_id),
    CONSTRAINT fk_iv_recipe_item_recipe FOREIGN KEY (recipe_id) REFERENCES iv_recipe(id) ON DELETE CASCADE,
    CONSTRAINT fk_iv_recipe_item_ingredient FOREIGN KEY (ingredient_id) REFERENCES iv_ingredient(id) ON DELETE RESTRICT,
    INDEX idx_iv_recipe_item_ingredient (ingredient_id),
    CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE iv_stock (
    id CHAR(36) NOT NULL PRIMARY KEY,
    branch_id CHAR(36) NOT NULL,
    ingredient_id CHAR(36) NOT NULL,
    quantity_on_hand DECIMAL(12,3) NOT NULL DEFAULT 0,
    reserved_quantity DECIMAL(12,3) NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_iv_stock_branch_ingredient (branch_id, ingredient_id),
    CONSTRAINT fk_iv_stock_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id) ON DELETE CASCADE,
    CONSTRAINT fk_iv_stock_ingredient FOREIGN KEY (ingredient_id) REFERENCES iv_ingredient(id) ON DELETE RESTRICT,
    INDEX idx_iv_stock_branch (branch_id),
    CHECK (quantity_on_hand >= 0),
    CHECK (reserved_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE iv_stock_movement (
    id CHAR(36) NOT NULL PRIMARY KEY,
    branch_id CHAR(36) NOT NULL,
    ingredient_id CHAR(36) NOT NULL,
    order_id CHAR(36) NULL,
    movement_type VARCHAR(40) NOT NULL,
    quantity DECIMAL(12,3) NOT NULL,
    before_quantity DECIMAL(12,3) NOT NULL,
    after_quantity DECIMAL(12,3) NOT NULL,
    reason VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    CONSTRAINT fk_iv_movement_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id) ON DELETE RESTRICT,
    CONSTRAINT fk_iv_movement_ingredient FOREIGN KEY (ingredient_id) REFERENCES iv_ingredient(id) ON DELETE RESTRICT,
    CONSTRAINT fk_iv_movement_order FOREIGN KEY (order_id) REFERENCES od_order(id) ON DELETE SET NULL,
    INDEX idx_iv_movement_branch_ingredient_created (branch_id, ingredient_id, created_at),
    INDEX idx_iv_movement_order (order_id),
    INDEX idx_iv_movement_type_created (movement_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
