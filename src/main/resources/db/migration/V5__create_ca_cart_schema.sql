-- Pine Drink - Cart schema

CREATE TABLE ca_cart (
    id CHAR(36) NOT NULL PRIMARY KEY,
    customer_id CHAR(36) NULL,
    branch_id CHAR(36) NOT NULL,
    session_id VARCHAR(120) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    CONSTRAINT fk_ca_cart_customer FOREIGN KEY (customer_id) REFERENCES cu_customer_profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_ca_cart_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id) ON DELETE RESTRICT,
    INDEX idx_ca_cart_customer_status (customer_id, status),
    INDEX idx_ca_cart_session_status (session_id, status),
    INDEX idx_ca_cart_branch (branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ca_cart_item (
    id CHAR(36) NOT NULL PRIMARY KEY,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
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
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
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
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    CONSTRAINT fk_ca_item_topping_item FOREIGN KEY (cart_item_id) REFERENCES ca_cart_item(id) ON DELETE CASCADE,
    CONSTRAINT fk_ca_item_topping_topping FOREIGN KEY (topping_id) REFERENCES pr_topping(id) ON DELETE RESTRICT,
    INDEX idx_ca_item_topping_item (cart_item_id),
    INDEX idx_ca_item_topping_topping (topping_id),
    CHECK (quantity > 0),
    CHECK (unit_price >= 0),
    CHECK (total_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
