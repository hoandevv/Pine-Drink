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

- `SYSTEM`: full app access across all branches
- `BRANCH`: selected branch only

There is no `BRAND` scope. Pine Drink is branch-first and no-brand.

## Service Rules

Use `AccessScopeService` in service layer for target-data checks:

```java
assertSystemAccess()
assertCanAccessBranch(String branchId)
assertCanManageBranch(String branchId)
assertCanDeleteBranch(String branchId)
```

Default behavior:
- View global catalog data: any user with matching `PERM_*` permission
- Manage global catalog data: `SYSTEM` scope
- Create branch: `SYSTEM` scope
- View branch: `SYSTEM` or matching `BRANCH`
- Update branch: `SYSTEM` or matching `BRANCH`
- Delete branch: `SYSTEM` or matching `BRANCH`
- View/manage order, stock, availability: `SYSTEM` or matching `BRANCH`

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

1. Add/verify permissions in migration seed files.
2. Replace role checks with `PERM_*` checks in controller.
3. Add branch scope check in service before branch-scoped data mutation/read.
4. Use `assertSystemAccess()` for global admin-only operations.
5. Compile with `./mvnw -q -DskipTests compile`.
6. Test both permission denial and out-of-scope denial.
