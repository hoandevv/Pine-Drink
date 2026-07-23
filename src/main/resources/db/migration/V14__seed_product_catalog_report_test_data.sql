-- Seed 2,000 products for testing PRODUCT_CATALOG report performance.
-- Uses INSERT IGNORE so local dev databases that already contain these codes do not fail.

CREATE TEMPORARY TABLE tmp_seed_numbers (
    n INT NOT NULL PRIMARY KEY
);

INSERT INTO tmp_seed_numbers (n)
SELECT d1.n + d2.n * 10 + d3.n * 100 + d4.n * 1000 + 1 AS n
FROM (
    SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) d1
CROSS JOIN (
    SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) d2
CROSS JOIN (
    SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) d3
CROSS JOIN (
    SELECT 0 n UNION ALL SELECT 1
) d4
WHERE d1.n + d2.n * 10 + d3.n * 100 + d4.n * 1000 + 1 <= 2000;

INSERT IGNORE INTO pr_category (
    id,
    code,
    name,
    description,
    display_order,
    status,
    created_at,
    updated_at
)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'TEST_CAT_TEA', 'Test Tea', 'Report test category', 101, 'ACTIVE', NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000002', 'TEST_CAT_COFFEE', 'Test Coffee', 'Report test category', 102, 'ACTIVE', NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000003', 'TEST_CAT_MILK_TEA', 'Test Milk Tea', 'Report test category', 103, 'ACTIVE', NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000004', 'TEST_CAT_SMOOTHIE', 'Test Smoothie', 'Report test category', 104, 'ACTIVE', NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000005', 'TEST_CAT_JUICE', 'Test Juice', 'Report test category', 105, 'ACTIVE', NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000006', 'TEST_CAT_SODA', 'Test Soda', 'Report test category', 106, 'ACTIVE', NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000007', 'TEST_CAT_YOGURT', 'Test Yogurt', 'Report test category', 107, 'ACTIVE', NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000008', 'TEST_CAT_TOPPING_DRINK', 'Test Topping Drink', 'Report test category', 108, 'ACTIVE', NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000009', 'TEST_CAT_SEASONAL', 'Test Seasonal', 'Report test category', 109, 'ACTIVE', NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000010', 'TEST_CAT_SIGNATURE', 'Test Signature', 'Report test category', 110, 'ACTIVE', NOW(), NOW());

INSERT IGNORE INTO pr_product (
    id,
    category_id,
    code,
    name,
    description,
    image_url,
    base_price,
    preparation_minutes,
    is_featured,
    is_best_seller,
    available_ice_levels,
    available_sugar_levels,
    status,
    created_at,
    updated_at
)
SELECT
    UUID(),
    CONCAT('10000000-0000-0000-0000-0000000000', LPAD(((n - 1) MOD 10) + 1, 2, '0')),
    CONCAT('TEST-PROD-', LPAD(n, 4, '0')),
    CONCAT('Test Product ', LPAD(n, 4, '0')),
    'Generated product for report performance testing',
    NULL,
    15000 + ((n MOD 40) * 1000),
    5 + (n MOD 16),
    FALSE,
    FALSE,
    '0,30,50,70,100',
    '0,30,50,70,100',
    'INACTIVE',
    NOW(),
    NOW()
FROM tmp_seed_numbers;

INSERT IGNORE INTO pr_product_variant (
    id,
    product_id,
    variant_code,
    variant_name,
    size_label,
    price_delta,
    display_order,
    status,
    created_at,
    updated_at
)
SELECT
    UUID(),
    p.id,
    v.variant_code,
    v.variant_name,
    v.size_label,
    v.price_delta,
    v.display_order,
    'ACTIVE',
    NOW(),
    NOW()
FROM pr_product p
JOIN tmp_seed_numbers n ON p.code = CONCAT('TEST-PROD-', LPAD(n.n, 4, '0'))
JOIN (
    SELECT 'S' AS variant_code, 'Small' AS variant_name, 'S' AS size_label, 0 AS price_delta, 1 AS display_order
    UNION ALL SELECT 'M', 'Medium', 'M', 5000, 2
    UNION ALL SELECT 'L', 'Large', 'L', 10000, 3
) v;

UPDATE pr_product
SET status = 'INACTIVE',
    is_featured = FALSE,
    is_best_seller = FALSE,
    updated_at = NOW()
WHERE code LIKE 'TEST-PROD-%';

UPDATE pr_category
SET status = 'INACTIVE',
    updated_at = NOW()
WHERE code LIKE 'TEST_CAT_%';

DROP TEMPORARY TABLE tmp_seed_numbers;
