# TDD - Core CRUD

## Domain Direction

Pine Drink is no-brand and branch-first. Java code and database must not contain Brand entity, Brand repository, `brand_id`, or `brandId` API fields.

## Components

- `BranchController`, `BranchService`, `BranchRepository`
- `ProductController`, `ProductService`, `ProductRepository`
- `CategoryRepository`
- `ToppingRepository`
- `BranchProductAvailabilityRepository`
- `BranchToppingAvailabilityRepository`

## Branch Design

Branch is top-level operational unit.

```text
ce_branch
  ce_branch_hours
  ce_pickup_time_slot
  od_order
  iv_stock
  mn_branch_product_availability
  mn_branch_topping_availability
```

Branch list endpoints use pagination:

```java
Page<Branch> findByStatus(String status, Pageable pageable);
```

## Product Design

Product catalog is global.

```text
pr_category -> pr_product -> pr_product_variant
pr_product -> pr_product_topping -> pr_topping
```

Product list supports optional category filter:

```java
Page<Product> findByCategoryId(String categoryId, Pageable pageable);
```

## Authorization

Use controller permission and service scope gates.

```java
@PreAuthorize("hasAuthority('PERM_PRODUCT_UPDATE')")
accessScopeService.assertSystemAccess();
```

For branch data:

```java
@PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
accessScopeService.assertCanManageBranch(branchId);
```

## Acceptance Criteria

- No Brand class/repository/controller/service exists.
- No `brandId` in request/response DTOs.
- No `findByBrand...` repository method exists.
- `GET /api/v1/branches` returns paged branches.
- `GET /api/v1/products` returns paged products.
- `GET /api/v1/products?categoryId=...` filters by category.
- Compile passes with `./mvnw -q -DskipTests compile`.
