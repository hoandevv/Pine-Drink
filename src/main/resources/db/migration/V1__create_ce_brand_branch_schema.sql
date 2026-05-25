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
