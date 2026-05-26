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
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    recipe_id CHAR(36) NOT NULL,
    ingredient_id CHAR(36) NOT NULL,
    quantity DECIMAL(12,3) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_iv_recipe_ingredient (recipe_id, ingredient_id),
    CONSTRAINT fk_iv_recipe_item_recipe FOREIGN KEY (recipe_id) REFERENCES iv_recipe(id) ON DELETE CASCADE,
    CONSTRAINT fk_iv_recipe_item_ingredient FOREIGN KEY (ingredient_id) REFERENCES iv_ingredient(id) ON DELETE RESTRICT,
    INDEX idx_iv_recipe_item_ingredient (ingredient_id),
    CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE iv_stock (
    id CHAR(36) NOT NULL PRIMARY KEY,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    branch_id CHAR(36) NOT NULL,
    ingredient_id CHAR(36) NOT NULL,
    quantity_on_hand DECIMAL(12,3) NOT NULL DEFAULT 0,
    reserved_quantity DECIMAL(12,3) NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_iv_stock_branch_ingredient (branch_id, ingredient_id),
    CONSTRAINT fk_iv_stock_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id) ON DELETE CASCADE,
    CONSTRAINT fk_iv_stock_ingredient FOREIGN KEY (ingredient_id) REFERENCES iv_ingredient(id) ON DELETE RESTRICT,
    INDEX idx_iv_stock_branch (branch_id),
    CHECK (quantity_on_hand >= 0),
    CHECK (reserved_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE iv_stock_movement (
    id CHAR(36) NOT NULL PRIMARY KEY,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
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
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    CONSTRAINT fk_iv_movement_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id) ON DELETE RESTRICT,
    CONSTRAINT fk_iv_movement_ingredient FOREIGN KEY (ingredient_id) REFERENCES iv_ingredient(id) ON DELETE RESTRICT,
    CONSTRAINT fk_iv_movement_order FOREIGN KEY (order_id) REFERENCES od_order(id) ON DELETE SET NULL,
    INDEX idx_iv_movement_branch_ingredient_created (branch_id, ingredient_id, created_at),
    INDEX idx_iv_movement_order (order_id),
    INDEX idx_iv_movement_type_created (movement_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
