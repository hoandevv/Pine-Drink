# Plan 12 - Permission Management

Pine Drink authorization uses permissions plus branch-first scope.

## Model

```text
Account -> AccountRoleAssignment -> Role
                              -> Scope
Role -> RolePermission -> Permission
```

## Scope Types

- `SYSTEM`: full app access across all branches
- `BRANCH`: assigned branch only

There is no `BRAND` scope.

## Permission Format

Permissions are stored in DB without prefix:

```text
PRODUCT_VIEW
BRANCH_UPDATE
ROLE_PERMISSION_UPDATE
```

Runtime authorities add `PERM_`:

```text
PERM_PRODUCT_VIEW
PERM_BRANCH_UPDATE
PERM_ROLE_PERMISSION_UPDATE
```

## Controller Rule

Use permission checks only.

```java
@PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
```

Avoid role checks in business controllers.

## Service Scope Rule

Global admin-only operations:

```java
accessScopeService.assertSystemAccess();
```

Branch operations:

```java
accessScopeService.assertCanAccessBranch(branchId);
accessScopeService.assertCanManageBranch(branchId);
accessScopeService.assertCanDeleteBranch(branchId);
```

## Roles

- `ADMIN`: normally `SYSTEM` scope
- `MANAGER`: `SYSTEM` or selected `BRANCH` scope depending assignment
- `DELIVERY`: normally selected `BRANCH` scope
- `CUSTOMER`: self-service permissions only

## Cache

- Cache key: `auth:permissions:{accountId}`
- Cache value: `PERM_*` authorities
- Invalidate cache on role assignment/revoke
- Invalidate affected users on role-permission update

## Acceptance Criteria

- No scope uses brand.
- Account search does not filter by brand.
- Role assignment accepts `scopeType` and optional `branchId`.
- Out-of-branch user cannot access branch data.
- Compile passes.
