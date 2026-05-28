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
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    voucher_id CHAR(36) NOT NULL,
    branch_id CHAR(36) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_vc_voucher_branch (voucher_id, branch_id),
    CONSTRAINT fk_vc_voucher_branch_voucher FOREIGN KEY (voucher_id) REFERENCES vc_voucher(id) ON DELETE CASCADE,
    CONSTRAINT fk_vc_voucher_branch_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vc_voucher_usage (
    id CHAR(36) NOT NULL PRIMARY KEY,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    voucher_id CHAR(36) NOT NULL,
    order_id CHAR(36) NOT NULL,
    customer_id CHAR(36) NULL,
    discount_amount DECIMAL(12,2) NOT NULL,
    used_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
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
    created_by CHAR(36) NULL,
    updated_by CHAR(36) NULL,
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
    created_by CHAR(36) NULL,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_py_transaction_code (transaction_code),
    CONSTRAINT fk_py_transaction_order FOREIGN KEY (order_id) REFERENCES od_order(id) ON DELETE RESTRICT,
    CONSTRAINT fk_py_transaction_intent FOREIGN KEY (payment_intent_id) REFERENCES py_payment_intent(id) ON DELETE SET NULL,
    INDEX idx_py_transaction_order (order_id),
    INDEX idx_py_transaction_status_created (status, created_at),
    CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE py_callback_log (
    id CHAR(36) NOT NULL PRIMARY KEY,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    provider VARCHAR(50) NOT NULL,
    transaction_code VARCHAR(100) NULL,
    request_id VARCHAR(120) NULL,
    raw_payload JSON NOT NULL,
    signature_valid BOOLEAN NOT NULL DEFAULT FALSE,
    processing_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    error_message VARCHAR(500) NULL,
    received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
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
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    completed_at DATETIME NULL,
    UNIQUE KEY uk_py_refund_code (refund_code),
    CONSTRAINT fk_py_refund_transaction FOREIGN KEY (transaction_id) REFERENCES py_transaction(id) ON DELETE RESTRICT,
    CONSTRAINT fk_py_refund_requested_by FOREIGN KEY (requested_by) REFERENCES ia_account(id) ON DELETE SET NULL,
    INDEX idx_py_refund_transaction (transaction_id),
    INDEX idx_py_refund_status (status),
    CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
