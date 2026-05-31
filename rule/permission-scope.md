# Permission And Scope Rule

Use this rule for backend authorization in Pine Drink.

## Core Pattern

Every protected business operation should pass two gates:

```text
Controller gate -> permission/action check
Service gate    -> scope/data check
```

Controller checks what user can do:

```java
@PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
```

Service checks where user can do it:

```java
accessScopeService.assertCanManageBranch(branchId);
```

Permission alone is not enough. Scope alone is not enough.

## Authority Naming

- Role authority format: `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_CUSTOMER`, `ROLE_DELIVERY`
- Permission authority format: `PERM_<MODULE>_<ACTION>`
- Controller permission checks must use `hasAuthority('PERM_*')`
- Avoid new `hasRole(...)` or `hasAnyRole(...)` checks for business modules

## Scope Meaning

- `SYSTEM`: all brands and branches
- `BRAND`: selected brand and all branches under that brand
- `BRANCH`: selected branch only

## Service Rules

Use `AccessScopeService` in service layer for target-data checks:

```java
assertCanAccessBrand(String brandId)
assertCanManageBrand(String brandId)
assertCanAccessBranch(String branchId)
assertCanManageBranch(String branchId)
assertCanDeleteBranch(String branchId)
```

Default behavior:
- View brand data: `SYSTEM` or matching `BRAND`
- Create branch: `SYSTEM` or matching `BRAND`
- View branch: `SYSTEM`, matching `BRAND`, or matching `BRANCH`
- Update branch: `SYSTEM`, matching `BRAND`, or matching `BRANCH`
- Delete branch: `SYSTEM` or matching `BRAND` only

## JWT Rule

- JWT carries identity and roles.
- Backend loads permissions from Redis/DB during request authentication.
- Always check account status before setting `SecurityContext`.
- If account is not `ACTIVE`, reject request and do not set authentication.

## Cache Rule

- Permission cache key: `auth:permissions:{accountId}`
- Cache stores `PERM_*` authorities.
- Assign/revoke role must invalidate target user permission cache.
- Future role-permission updates must invalidate caches for all accounts assigned to changed role.

## Migration Rule

When migrating a controller:

1. Add/verify permissions in `V11__seed_permissions_and_role_permissions.sql`.
2. Replace role checks with `PERM_*` checks in controller.
3. Add scope check in service before data mutation/read.
4. Compile with `./mvnw -q -DskipTests compile`.
5. Test both permission denial and out-of-scope denial.
