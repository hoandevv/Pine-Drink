-- Pine Drink - Daily sellable stock schema
-- Tracks how many cups of each product variant a branch can sell per day.

CREATE TABLE ce_branch_variant_daily_stock (
    id CHAR(36) NOT NULL PRIMARY KEY,
    branch_id CHAR(36) NOT NULL,
    variant_id CHAR(36) NOT NULL,
    stock_date DATE NOT NULL,
    daily_quantity INT NOT NULL DEFAULT 0,
    sold_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_ce_branch_variant_stock_date (branch_id, variant_id, stock_date),
    CONSTRAINT fk_ce_branch_variant_daily_stock_branch
        FOREIGN KEY (branch_id) REFERENCES ce_branch(id),
    CONSTRAINT fk_ce_branch_variant_daily_stock_variant
        FOREIGN KEY (variant_id) REFERENCES pr_product_variant(id)
    INDEX idx_ce_branch_variant_daily_stock_branch_date (branch_id, stock_date),
    INDEX idx_ce_branch_variant_daily_stock_variant_date (variant_id, stock_date),
    CHECK (daily_quantity >= 0),
    CHECK (sold_quantity >= 0),
    CHECK (reserved_quantity >= 0),
    CHECK (sold_quantity + reserved_quantity <= daily_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ce_branch_variant_stock_log (
    id CHAR(36) NOT NULL PRIMARY KEY,
    daily_stock_id CHAR(36) NOT NULL,
    order_id CHAR(36) NULL,
    action_type VARCHAR(40) NOT NULL,
    quantity INT NOT NULL,
    before_daily_quantity INT NOT NULL,
    after_daily_quantity INT NOT NULL,
    before_sold_quantity INT NOT NULL,
    after_sold_quantity INT NOT NULL,
    before_reserved_quantity INT NOT NULL,
    after_reserved_quantity INT NOT NULL,
    reason VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    CONSTRAINT fk_ce_branch_variant_stock_log_daily_stock FOREIGN KEY (daily_stock_id) REFERENCES ce_branch_variant_daily_stock(id) ON DELETE CASCADE,
    CONSTRAINT fk_ce_branch_variant_stock_log_order
        FOREIGN KEY (order_id) REFERENCES od_order(id) ON DELETE SET NULL
    INDEX idx_ce_branch_variant_stock_log_stock_created (daily_stock_id, created_at),
    INDEX idx_ce_branch_variant_stock_log_order (order_id),
    INDEX idx_ce_branch_variant_stock_log_action_created (action_type, created_at),
    CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
