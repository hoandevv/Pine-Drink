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
