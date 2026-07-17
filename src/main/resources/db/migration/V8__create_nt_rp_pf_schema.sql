-- Pine Drink - Notification, Report, Platform and Chat schema

CREATE TABLE nt_notification (
    id CHAR(36) NOT NULL PRIMARY KEY,
    recipient_account_id CHAR(36) NULL,
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
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    CONSTRAINT fk_nt_notification_account FOREIGN KEY (recipient_account_id) REFERENCES ia_account(id) ON DELETE CASCADE,
    CONSTRAINT fk_nt_notification_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id) ON DELETE CASCADE,
    INDEX idx_nt_notification_recipient_status_created (recipient_account_id, status, created_at),
    INDEX idx_nt_notification_reference (reference_type, reference_id),
    INDEX idx_nt_notification_branch_created (branch_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE nt_template (
    id CHAR(36) NOT NULL PRIMARY KEY,
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
    UNIQUE KEY uk_nt_template_code_channel (template_code, channel),
    INDEX idx_nt_template_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ch_room (
    id CHAR(36) NOT NULL PRIMARY KEY,
    room_code VARCHAR(50) NOT NULL,
    room_type VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER_SUPPORT',
    customer_account_id CHAR(36) NOT NULL,
    branch_id CHAR(36) NULL,
    order_id CHAR(36) NULL,
    title VARCHAR(150) NULL,
    last_message_at DATETIME NULL,
    last_message_preview VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_ch_room_code (room_code),
    UNIQUE KEY uk_ch_room_order_customer (order_id, customer_account_id),
    INDEX idx_ch_room_customer_status (customer_account_id, status),
    INDEX idx_ch_room_branch_status (branch_id, status),
    INDEX idx_ch_room_last_message_at (last_message_at),
    CONSTRAINT fk_ch_room_customer_account FOREIGN KEY (customer_account_id) REFERENCES ia_account(id) ON DELETE CASCADE,
    CONSTRAINT fk_ch_room_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id) ON DELETE SET NULL,
    CONSTRAINT fk_ch_room_order FOREIGN KEY (order_id) REFERENCES od_order(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ch_message (
    id CHAR(36) NOT NULL PRIMARY KEY,
    room_id CHAR(36) NOT NULL,
    sender_account_id CHAR(36) NOT NULL,
    message_type VARCHAR(30) NOT NULL DEFAULT 'TEXT',
    content TEXT NULL,
    metadata JSON NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    INDEX idx_ch_message_room_created_at (room_id, created_at),
    INDEX idx_ch_message_sender_created_at (sender_account_id, created_at),
    CONSTRAINT fk_ch_message_room FOREIGN KEY (room_id) REFERENCES ch_room(id) ON DELETE CASCADE,
    CONSTRAINT fk_ch_message_sender_account FOREIGN KEY (sender_account_id) REFERENCES ia_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rp_export_request (
    id CHAR(36) NOT NULL PRIMARY KEY,
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
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    CONSTRAINT fk_rp_export_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id) ON DELETE SET NULL,
    CONSTRAINT fk_rp_export_requested_by FOREIGN KEY (requested_by) REFERENCES ia_account(id) ON DELETE RESTRICT,
    INDEX idx_rp_export_requested_status (requested_by, status, requested_at),
    INDEX idx_rp_export_type_requested (report_type, requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pf_outbox_event (
    id CHAR(36) NOT NULL PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id CHAR(36) NOT NULL,
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
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
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
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_pf_idempotency_key (idempotency_key),
    INDEX idx_pf_idempotency_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
