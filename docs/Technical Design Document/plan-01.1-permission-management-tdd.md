# TDD - Permission Management

## Goal

Implement authorization using `PERM_*` permissions and branch-first data scope.

## Scope Types

```text
SYSTEM -> all branches
BRANCH -> one branch
```

No `BRAND` scope.

## Controller Gate

Controllers use Spring Security authorities:

```java
@PreAuthorize("hasAuthority('PERM_ACCOUNT_VIEW')")
@PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
@PreAuthorize("hasAuthority('PERM_PRODUCT_VIEW')")
```

## Service Gate

Service checks target data scope:

```java
accessScopeService.assertSystemAccess();
accessScopeService.assertCanAccessBranch(branchId);
accessScopeService.assertCanManageBranch(branchId);
accessScopeService.assertCanDeleteBranch(branchId);
```

## Branch Endpoints

```http
GET /api/v1/branches
GET /api/v1/branches/active
GET /api/v1/branches/{id}
POST /api/v1/branches
PUT /api/v1/branches/{id}
PATCH /api/v1/branches/{id}/status
DELETE /api/v1/branches/{id}
```

## Product Endpoints

Product catalog is global. Product mutation requires `SYSTEM` scope.

```http
GET /api/v1/products
GET /api/v1/products?categoryId={categoryId}
POST /api/v1/products
PUT /api/v1/products/{id}
PATCH /api/v1/products/{id}/status
DELETE /api/v1/products/{id}
```

## Account Role Assignment

Request supports:

```json
{
  "roleCode": "MANAGER",
  "scopeType": "BRANCH",
  "branchId": "...",
  "expiresAt": null
}
```

For system-wide admin:

```json
{
  "roleCode": "ADMIN",
  "scopeType": "SYSTEM",
  "branchId": null,
  "expiresAt": null
}
```

## Acceptance Criteria

- DB has no `BRAND` scope row.
- Java constants have no `SCOPE_BRAND`.
- No service calls `assertCanAccessBrand` or `assertCanManageBrand`.
- Compile passes.
