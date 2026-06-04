# Branch First Rule

Use this rule for Pine Drink domain design and API behavior.

## Domain Model

Pine Drink runs as one system with many branches. There is no `Brand` model in DB or Java code.

```text
Pine Drink
  Branch: District 1
  Branch: District 7
  Branch: Thu Duc
```

## Ownership Rule

Global catalog/config data does not store `brand_id`:

- Product
- Category
- Topping
- Voucher
- Ingredient
- Template

Branch-scoped data stores `branch_id`:

- Branch hours
- Pickup time slots
- Settings when branch-specific
- Cart
- Order
- Order delivery
- Stock
- Stock movement
- Product availability
- Topping availability
- Notification/report/outbox when branch-specific
- Account role scope

## API Rule

Do not accept or require `brandId` in normal APIs.

Good:

```http
GET /api/v1/products?page=0&size=20
GET /api/v1/products?categoryId={categoryId}
GET /api/v1/branches?page=0&size=20
GET /api/v1/branches/active?page=0&size=20
```

Avoid:

```http
GET /api/v1/products?brandId={brandId}
GET /api/v1/branches/brand/{brandId}
```

## Service Rule

Use global repository methods for catalog data.

```java
Page<Product> products = productRepository.findAll(pageable);
Page<Product> products = productRepository.findByCategoryId(categoryId, pageable);
```

Use branch repository methods for branch data.

```java
accessScopeService.assertCanAccessBranch(branchId);
Page<Order> orders = orderRepository.findByBranchId(branchId, pageable);
```

## Scope Rule

Permission scope uses only:

- `SYSTEM`: full app access
- `BRANCH`: assigned branch only

Managers with all-branch access use `SYSTEM`. Staff/delivery roles normally use `BRANCH`.

## UI Rule

Do not show brand selector.

Show branch selector only for branch-specific operations:

- order handling
- delivery assignment
- stock management
- product/topping availability
- branch schedule
- staff assignment

## Migration Rule

No-brand migration must remove:

1. `ce_brand` and `ce_brand_domain`
2. every `brand_id` column
3. every FK/index/unique key based on brand
4. Java `Brand` and `BrandDomain` entities/repositories
5. `BRAND` scope logic

Compile with `./mvnw -q -DskipTests compile` after code changes.
