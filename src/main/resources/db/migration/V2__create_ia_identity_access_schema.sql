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
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    role_id CHAR(36) NOT NULL,
    permission_id CHAR(36) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
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
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
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
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_ia_account_role_scope (account_id, role_id, scope_id),
    CONSTRAINT fk_ia_ara_account FOREIGN KEY (account_id) REFERENCES ia_account(id) ON DELETE CASCADE,
    CONSTRAINT fk_ia_ara_role FOREIGN KEY (role_id) REFERENCES ia_role(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ia_ara_scope FOREIGN KEY (scope_id) REFERENCES ia_scope(id) ON DELETE RESTRICT,
    INDEX idx_ia_ara_account_status (account_id, status),
    INDEX idx_ia_ara_role_scope (role_id, scope_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ia_audit_log (
    id CHAR(36) NOT NULL PRIMARY KEY,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
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
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    CONSTRAINT fk_ia_audit_actor FOREIGN KEY (actor_account_id) REFERENCES ia_account(id) ON DELETE SET NULL,
    INDEX idx_ia_audit_actor_created (actor_account_id, created_at),
    INDEX idx_ia_audit_target (target_type, target_id),
    INDEX idx_ia_audit_brand_branch_created (brand_id, branch_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ia_refresh_token (
    id CHAR(36) NOT NULL PRIMARY KEY,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    account_id CHAR(36) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    device_info VARCHAR(255) NULL,
    ip_address VARCHAR(64) NULL,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    CONSTRAINT fk_ia_refresh_token_account FOREIGN KEY (account_id) REFERENCES ia_account(id) ON DELETE CASCADE,
    UNIQUE KEY uk_ia_refresh_token_hash (token_hash),
    INDEX idx_ia_refresh_token_account_expires (account_id, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
