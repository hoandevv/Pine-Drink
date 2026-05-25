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
