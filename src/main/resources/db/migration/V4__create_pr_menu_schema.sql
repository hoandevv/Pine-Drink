-- Pine Drink - Product Catalog and Menu schema

CREATE TABLE pr_category (
    id CHAR(36) NOT NULL PRIMARY KEY,
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
    UNIQUE KEY uk_pr_category_code (code),
    INDEX idx_pr_category_status_order (status, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pr_product (
    id CHAR(36) NOT NULL PRIMARY KEY,
    category_id CHAR(36) NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    image_url VARCHAR(500) NULL,
    base_price DECIMAL(12,2) NOT NULL,
    preparation_minutes INT NOT NULL DEFAULT 10,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    is_best_seller BOOLEAN NOT NULL DEFAULT FALSE,
    available_ice_levels VARCHAR(50) NOT NULL DEFAULT '0,30,50,70,100',
    available_sugar_levels VARCHAR(50) NOT NULL DEFAULT '0,30,50,70,100',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_pr_product_code (code),
    CONSTRAINT fk_pr_product_category FOREIGN KEY (category_id) REFERENCES pr_category(id) ON DELETE RESTRICT,
    INDEX idx_pr_product_category_status (category_id, status),
    INDEX idx_pr_product_status (status),
    INDEX idx_pr_product_featured (is_featured, status),
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
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    image_url VARCHAR(500) NULL,
    group_name VARCHAR(100) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_pr_topping_code (code),
    INDEX idx_pr_topping_status (status),
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
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_pr_product_topping (product_id, topping_id),
    CONSTRAINT fk_pr_product_topping_product FOREIGN KEY (product_id) REFERENCES pr_product(id) ON DELETE CASCADE,
    CONSTRAINT fk_pr_product_topping_topping FOREIGN KEY (topping_id) REFERENCES pr_topping(id) ON DELETE RESTRICT,
    INDEX idx_pr_product_topping_product_status (product_id, status),
    INDEX idx_pr_product_topping_topping (topping_id),
    CHECK (max_quantity >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE mn_branch_product_availability (
    id CHAR(36) NOT NULL PRIMARY KEY,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    branch_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    sale_price DECIMAL(12,2) NULL,
    sold_out_reason VARCHAR(255) NULL,
    available_from DATETIME NULL,
    available_to DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
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
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    branch_id CHAR(36) NOT NULL,
    topping_id CHAR(36) NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    sold_out_reason VARCHAR(255) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    UNIQUE KEY uk_mn_branch_topping (branch_id, topping_id),
    CONSTRAINT fk_mn_branch_topping_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id) ON DELETE CASCADE,
    CONSTRAINT fk_mn_branch_topping_topping FOREIGN KEY (topping_id) REFERENCES pr_topping(id) ON DELETE CASCADE,
    INDEX idx_mn_branch_topping_branch_available (branch_id, is_available)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
