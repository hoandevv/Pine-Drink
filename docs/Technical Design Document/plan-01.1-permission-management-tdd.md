# Technical Design Document - Permission Management Foundation

| Field | Value |
|---|---|
| Document ID | TDD-PINE-PERM-01.1 |
| Project | Pine Drink |
| Module | Permission Management Foundation |
| Version | 1.1 |
| Status | Implemented |
| Last Updated | 2026-05-31 |

---

## 1. Purpose

Tai lieu nay tong hop permission foundation hien tai cua Pine Drink sau khi nang cap authentication, permission gate va scope gate.

Muc tieu chinh:
- Chuyen authorization tu role-only sang `permission + scope`.
- Giu role trong JWT de frontend va code cu van dung duoc.
- Load permission tu DB/cache moi request de backend khong phu thuoc permission claim trong token.
- Tach ro action permission va data scope.
- Bat dau migrate module `Account` va `Branch` sang permission-based checks.

---

## 2. Authorization Model

He thong dung 3 lop:

```text
Role       -> nhom nguoi dung: ADMIN, MANAGER, CUSTOMER, DELIVERY
Permission -> action cu the: ACCOUNT_VIEW, BRANCH_UPDATE, ...
Scope      -> pham vi du lieu: SYSTEM, BRAND, BRANCH
```

Quyet dinh allow/deny dung pattern:

```text
Controller -> check action bang @PreAuthorize(hasAuthority('PERM_*'))
Service    -> check target data bang AccessScopeService hoac helper scope noi bo
```

Permission khong thay the scope. User co `PERM_BRANCH_UPDATE` van chi duoc update branch nam trong scope duoc gan.

---

## 3. Runtime Authentication Flow

Flow hien tai:

```text
Request co Bearer JWT
        |
        v
JwtAuthFilter
        |
        +--> validate token signature/expiry
        +--> parse subject accountId
        +--> load Account tu DB
        +--> load roles tu JWT claim roles
        +--> load permissions tu PermissionCacheService
        +--> build UserPrincipal(ROLE_* + PERM_*)
        +--> check account ACTIVE
        +--> set SecurityContext neu hop le
```

Neu account khong `ACTIVE`, filter tra `403` va khong set authentication.

---

## 4. JWT Strategy

Access token hien tai chua:
- `sub`
- `username`
- `email`
- `roles`

Access token khong chua `permissions` trong implementation hien tai.

Ly do:
- Role trong JWT giup frontend render nhanh va giu backward compatibility.
- Permission duoc load tu Redis/DB moi request qua `PermissionCacheService`.
- Khi role-permission thay doi, backend co the cap nhat permission nhanh hon token lifetime thong qua cache invalidation/TTL.

Luu y: role trong JWT co the stale cho role-based endpoint den khi access token het han. Vi vay controller moi nen dung `PERM_*` thay vi `ROLE_*` neu can dynamic permission.

---

## 5. Database Foundation

Permission foundation dung schema co san:
- `ia_permission`
- `ia_role_permission`
- `ia_scope`
- `ia_account_role_assignment`

Seed chinh thuc:
- `src/main/resources/db/migration/V11__seed_permissions_and_role_permissions.sql`

Module da seed permission:
- `ACCOUNT`
- `BRANCH`
- `PROFILE`
- `CUSTOMER_ADDRESS`
- `FILE`
- `GEOCODING`

Branch permissions da co:
- `BRANCH_VIEW`
- `BRANCH_CREATE`
- `BRANCH_UPDATE`
- `BRANCH_DELETE`

Runtime authority format:
- Role -> `ROLE_ADMIN`, `ROLE_MANAGER`, ...
- Permission -> `PERM_ACCOUNT_VIEW`, `PERM_BRANCH_UPDATE`, ...

---

## 6. Implemented Components

### 6.1 Permission loading

Files:
- `src/main/java/com/hoandev/pinedrink/repository/RolePermissionRepository.java`
- `src/main/java/com/hoandev/pinedrink/service/PermissionCacheService.java`
- `src/main/java/com/hoandev/pinedrink/security/CustomUserDetailsService.java`

Behavior:
- Query active permission codes theo active role assignments.
- Prefix permission thanh `PERM_*`.
- Cache permission authorities tai Redis key `auth:permissions:{accountId}` trong 15 phut.
- Invalidate cache khi assign/revoke role trong account management flow.

### 6.2 Principal model

File:
- `src/main/java/com/hoandev/pinedrink/security/UserPrincipal.java`

Behavior:
- Giu `id`, `username`, `email`, `password`, `status`, `authorities`.
- `getRoleAuthorities()` loc `ROLE_*`.
- `getPermissionAuthorities()` loc `PERM_*`.
- `isEnabled()` chi true khi status la `ACTIVE`.

### 6.3 JWT authentication hardening

File:
- `src/main/java/com/hoandev/pinedrink/security/JwtAuthFilter.java`

Da fix bug:

```text
JWT hop le
-> load user
-> build UserPrincipal
-> check ACTIVE
-> ACTIVE moi setAuthentication
-> INACTIVE/LOCKED thi reject, khong vao SecurityContext
```

Ket qua:
- Account inactive/locked khong the vao system bang token con han.
- Response hien tai dung `403` voi error code `AUTH_006` khi `UserPrincipal.isEnabled()` false.

---

## 7. Controller Migration Status

### 7.1 AccountController

File:
- `src/main/java/com/hoandev/pinedrink/controller/AccountController.java`

Permission mapping:

| Endpoint | Permission |
|---|---|
| `GET /api/v1/accounts` | `PERM_ACCOUNT_VIEW` |
| `GET /api/v1/accounts/{id}` | `PERM_ACCOUNT_VIEW` |
| `POST /api/v1/accounts` | `PERM_ACCOUNT_CREATE` |
| `PUT /api/v1/accounts/{id}` | `PERM_ACCOUNT_UPDATE` |
| `PATCH /api/v1/accounts/{id}/status` | `PERM_ACCOUNT_CHANGE_STATUS` |
| `POST /api/v1/accounts/{id}/reset-password` | `PERM_ACCOUNT_RESET_PASSWORD` |
| `GET /api/v1/accounts/{id}/roles` | `PERM_ACCOUNT_ROLE_VIEW` |
| `POST /api/v1/accounts/{id}/roles` | `PERM_ACCOUNT_ROLE_ASSIGN` |
| `DELETE /api/v1/accounts/{id}/roles/{assignmentId}` | `PERM_ACCOUNT_ROLE_REVOKE` |

### 7.2 BranchController

File:
- `src/main/java/com/hoandev/pinedrink/controller/BranchController.java`

Da migrate tu role-based sang permission-based:

| Endpoint | Permission |
|---|---|
| `POST /api/v1/branches` | `PERM_BRANCH_CREATE` |
| `PUT /api/v1/branches/{id}` | `PERM_BRANCH_UPDATE` |
| `DELETE /api/v1/branches/{id}` | `PERM_BRANCH_DELETE` |
| `GET /api/v1/branches/{id}` | `PERM_BRANCH_VIEW` |
| `GET /api/v1/branches/brand/{brandId}` | `PERM_BRANCH_VIEW` |
| `GET /api/v1/branches/brand/{brandId}/active` | `PERM_BRANCH_VIEW` |

---

## 8. Scope Enforcement

### 8.1 Account scope

File:
- `src/main/java/com/hoandev/pinedrink/service/impl/AccountServiceImpl.java`

Account service hien co scope helpers noi bo:
- `assertCanAccessAccount(...)`
- `assertCanManageTargetScope(...)`
- `assertCanAccessScope(...)`
- `resolveAccessScope()`

Rule:
- `SYSTEM` scope -> full access.
- `BRAND`/`BRANCH` scope -> chi thao tac trong brand/branch duoc gan.

### 8.2 Shared AccessScopeService

Files:
- `src/main/java/com/hoandev/pinedrink/service/AccessScopeService.java`
- `src/main/java/com/hoandev/pinedrink/service/impl/AccessScopeServiceImpl.java`

Purpose:
- Gom logic data-scope dung chung cho service layer.
- Tranh moi service tu viet scope rule rieng va bi lech behavior.

Public methods:

```java
void assertCanAccessBrand(String brandId);
void assertCanManageBrand(String brandId);
void assertCanAccessBranch(String branchId);
void assertCanManageBranch(String branchId);
void assertCanDeleteBranch(String branchId);
```

Rule hien tai:
- `SYSTEM` scope -> full access.
- `BRAND` scope -> access/manage brand do va branch thuoc brand do.
- `BRANCH` scope -> access/manage dung branch do.
- Delete branch -> chi `SYSTEM` hoac `BRAND`, khong cho `BRANCH` scope delete.

### 8.3 BranchService scope integration

File:
- `src/main/java/com/hoandev/pinedrink/service/impl/BranchServiceImpl.java`

Scope checks:
- `create()` -> `assertCanManageBrand(request.getBrandId())`
- `update()` -> `assertCanManageBranch(id)`
- `delete()` -> `assertCanDeleteBranch(id)`
- `getById()` -> `assertCanAccessBranch(id)`
- `getAllByBrandId()` -> `assertCanAccessBrand(brandId)`
- `getAllActiveByBrandId()` -> `assertCanAccessBrand(brandId)`

---

## 9. Security Notes

Known important behaviors:
- Backend reloads permissions from DB/cache each request.
- Role claim in JWT can be stale until token expiry.
- Permission cache TTL is 15 minutes.
- Role assign/revoke invalidates permission cache for target account.
- Role-permission CRUD is not implemented yet; if added later, must invalidate users assigned to changed role.
- Private file endpoint still needs separate review if expected to be authenticated-only.
- Reset-token request authentication should be constrained to reset-password flow if not already handled at endpoint layer.

---

## 10. Verification

Compile command:

```bash
./mvnw -q -DskipTests compile
```

Verified after current changes.

Recommended manual checks:
- Login active account -> protected endpoint OK.
- Change account status to `INACTIVE` or `LOCKED` -> same token must be rejected.
- Account without `PERM_BRANCH_UPDATE` -> `PUT /api/v1/branches/{id}` returns 403.
- Account with `PERM_BRANCH_UPDATE` but wrong brand/branch scope -> service returns insufficient permission.
- Account with `BRAND` scope -> can manage branches under that brand only.
- Account with `BRANCH` scope -> can view/update assigned branch only, cannot delete branch.

---

## 11. Next Steps

- Move duplicated account scope helpers to `AccessScopeService` when stable.
- Migrate remaining controllers from `hasRole`/`hasAnyRole` to `PERM_*`.
- Add role-permission management API with cache invalidation by role.
- Add `PermissionCodes` constants to reduce hardcoded `PERM_*` strings.
- Add integration tests for permission + scope combinations.
