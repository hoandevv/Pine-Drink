# Technical Design Document — Permission Management Foundation

| **Field** | **Value** |
|-----------|-----------|
| **Document ID** | TDD-PINE-PERM-012 |
| **Project** | Pine Drink — He thong order do uong online |
| **Module** | Permission Management Foundation |
| **Version** | 1.0 |
| **Status** | Implemented |
| **Author** | Pine Drink Dev Team |
| **Last Updated** | 2026-05-30 |

---

## 1. Title

**Bo sung nen tang phan quyen theo permission + scope cho Pine Drink**

---

## 2. Overview

Tai lieu nay mo ta chi tiet phan permission foundation vua duoc trien khai trong Pine Drink. Muc tieu la mo rong he thong tu `role-first` sang mo hinh `role + permission + scope`, nhung van giu duoc cach van hanh hien tai va khong pha vo cac module dang chay.

Ban trien khai hien tai tap trung vao 3 phan:
- seed permission va role-permission matrix chinh thuc
- load `PERM_*` authority vao security context khi login/refresh/request
- migrate module `Account` sang check permission tai controller, ket hop scope validation tai service

---

## 3. Purpose

- Chuan hoa quyen theo action nghiep vu, khong phu thuoc hoan toan vao role
- Cho phep mo rong sau nay cho `Order`, `Delivery`, `Inventory`, `Report`
- Giu frontend co the render nhanh theo role, dong thoi cho phep nang cap sang permission-based UI sau nay
- Giu backend authorization an toan hon thong qua `hasAuthority('PERM_*')` + scope check

---

## 4. Scope

### Trong pham vi (In Scope)
- Migration `V11` cho permission va role-permission matrix
- Load permission vao `UserPrincipal`
- Them claim `permissions` vao access token
- Migrate `AccountController` sang `@PreAuthorize(hasAuthority(...))`
- Giu scope validation tai `AccountServiceImpl`

### Ngoai pham vi (Out of Scope)
- UI quan ly permission
- API CRUD role-permission runtime
- Migrate toan bo controller khac sang permission
- Dynamic custom role editor
- Audit log chi tiet cho thay doi permission

---

## 5. Audience

| Doi tuong | Vai tro |
|-----------|---------|
| Developer Backend | Implement/migrate permission checks |
| Developer Frontend | Doc role/permission tu token de render UI |
| QA / Tester | Kiem thu authorization va scope behavior |
| Technical Leader | Review huong mo rong permission architecture |

---

## 6. Background

### 6.1 Van de truoc khi trien khai

He thong truoc day chu yeu authorize theo role:
- `hasRole('ADMIN')`
- `hasAnyRole('ADMIN', 'MANAGER')`

Mo hinh nay du dung cho giai doan dau, nhung se gap han che khi:
- 1 role can bi tach quyen chi tiet hon trong cung 1 module
- can them `DELIVERY`, `MANAGER`, `BRANCH ADMIN` ma khong muon hardcode qua nhieu `if role == ...`
- can bo sung quyen theo action nhu `ACCOUNT_RESET_PASSWORD`, `ACCOUNT_ROLE_ASSIGN`, `ORDER_ASSIGN_DELIVERY`

### 6.2 Nen tang da co san

- `ia_permission` va `ia_role_permission` da ton tai trong `src/main/resources/db/migration/V2__create_ia_identity_access_schema.sql`
- role bootstrap trong `src/main/resources/db/migration/V10__init_data.sql`
- scope model da ton tai va dang duoc dung tai service layer

---

## 7. Requirements

### 7.1 Functional Requirements

| # | Yeu cau | Mo ta |
|---|---------|-------|
| FR1 | Seed permission | Co danh sach permission chinh thuc theo module dang implement |
| FR2 | Seed role-permission | Role mac dinh duoc map den permission phu hop |
| FR3 | Load permission | Khi authenticate, principal phai co ca `ROLE_*` va `PERM_*` |
| FR4 | JWT claim | Access token phai co claim `permissions` |
| FR5 | Controller permission check | Module `Account` phai check bang permission thay vi role |
| FR6 | Scope safety | Doi voi action nhay cam, permission phai ket hop scope validation |

### 7.2 Non-Functional Requirements

| # | Yeu cau | Mo ta |
|---|---------|-------|
| NFR1 | Backward compatibility | Role claim van ton tai cho frontend va code cu |
| NFR2 | Security | Khong chi dua vao UI hien/ an menu; backend phai check permission that |
| NFR3 | Extensibility | De mo rong sang branch/order/delivery ma khong refactor lon |
| NFR4 | Deterministic seed | Permission seed dung UUID co dinh, co the reset DB de tao moi on dinh |

---

## 8. Design / Giai phap thiet ke

### 8.1 Authorization Model

```
Role       -> nhom van hanh (ADMIN, MANAGER, CUSTOMER, DELIVERY)
Permission -> hanh dong cu the (ACCOUNT_VIEW, ACCOUNT_UPDATE, ...)
Scope      -> pham vi du lieu (SYSTEM, BRAND, BRANCH)
```

Quyet dinh authorize day du can du 3 lop:
- co role phu hop de duoc map permission
- co permission phu hop cho action
- co scope phu hop cho target data

### 8.2 Runtime Flow

```
Login / Request with JWT
        |
        v
JwtAuthFilter
        |
        v
CustomUserDetailsService
        |
        +--> load active role assignments
        +--> load active permission codes from role_permission
        |
        v
UserPrincipal(authorities = ROLE_* + PERM_*)
        |
        v
SecurityContext
        |
        +--> Controller: @PreAuthorize(hasAuthority('PERM_*'))
        +--> Service: assertCanAccessAccount / assertCanManageTargetScope
```

### 8.3 JWT Claim Strategy

Access token hien tai chua:
- `sub`
- `username`
- `email`
- `roles`
- `permissions`

Muc dich:
- frontend co the doc role ngay de render menu
- permission co san trong token de debug/inspect va phuc vu render chi tiet sau nay
- backend van rebuild principal tu DB moi request de tranh phu thuoc hoan toan vao token claims

### 8.4 Phan biet controller va service checks

**Controller**
- check coarse-grained access
- vi du: `PERM_ACCOUNT_UPDATE`

**Service**
- check target-specific access
- vi du: account brand A co duoc sua account brand B hay khong

Permission khong thay the scope validation.

---

## 9. Implemented Database Design

### 9.1 Migration file

- `src/main/resources/db/migration/V11__seed_permissions_and_role_permissions.sql`

Migration nay da duoc chuan hoa lai tu file sample cu, va hien la seed chinh thuc cho permission foundation.

### 9.2 Implemented modules in V11

Permission hien duoc seed cho cac module dang co API thuc te:
- `ACCOUNT`
- `BRANCH`
- `PROFILE`
- `CUSTOMER_ADDRESS`
- `FILE`
- `GEOCODING`

### 9.3 Seeded ACCOUNT permissions

- `ACCOUNT_VIEW`
- `ACCOUNT_CREATE`
- `ACCOUNT_UPDATE`
- `ACCOUNT_CHANGE_STATUS`
- `ACCOUNT_RESET_PASSWORD`
- `ACCOUNT_ROLE_VIEW`
- `ACCOUNT_ROLE_ASSIGN`
- `ACCOUNT_ROLE_REVOKE`

### 9.4 Seeded role-permission matrix

**ADMIN**
- full permission tren cac module da seed trong V11

**MANAGER**
- `ACCOUNT_VIEW`
- `ACCOUNT_ROLE_VIEW`
- `BRANCH_*`
- bo utility `PROFILE_*`, `FILE_PRIVATE_VIEW`, `GEOCODING_*`

**CUSTOMER**
- bo self-service `PROFILE_*`, `CUSTOMER_ADDRESS_*`, `FILE_PRIVATE_VIEW`, `GEOCODING_*`

**DELIVERY**
- bo self-service/utility toi thieu `PROFILE_*`, `FILE_PRIVATE_VIEW`, `GEOCODING_*`

---

## 10. Implemented Code Changes

### 10.1 Repository layer

**File:** `src/main/java/com/hoandev/pinedrink/repository/RolePermissionRepository.java`

Bo sung query:

```java
List<String> findActivePermissionCodesByAccountId(String accountId, LocalDateTime now)
```

Muc dich:
- lay tat ca permission code active theo account thong qua role assignments + role_permission
- chi lay assignment con hieu luc

### 10.2 Security principal loading

**File:** `src/main/java/com/hoandev/pinedrink/security/CustomUserDetailsService.java`

Bo sung:
- load role authorities thanh `ROLE_*`
- load permission authorities thanh `PERM_*`
- hop nhat vao `UserPrincipal`

Core method moi:

```java
public UserPrincipal buildPrincipal(Account account)
```

Method nay duoc tai su dung boi:
- login flow
- refresh flow
- JWT request authentication flow

### 10.3 UserPrincipal enhancement

**File:** `src/main/java/com/hoandev/pinedrink/security/UserPrincipal.java`

Bo sung helper:
- `getRoleAuthorities()`
- `getPermissionAuthorities()`

Muc dich:
- giup JWT builder tach claim `roles` va `permissions`
- giu internal authority list thong nhat

### 10.4 JWT generation

**File:** `src/main/java/com/hoandev/pinedrink/security/JwtTokenProvider.java`

Thay doi:
- claim `roles` chi chua `ROLE_*`
- claim moi `permissions` chua `PERM_*`

Vi du payload:

```json
{
  "sub": "account-id",
  "username": "admin",
  "email": "admin@pine-drink.com",
  "roles": ["ROLE_ADMIN"],
  "permissions": ["PERM_ACCOUNT_VIEW", "PERM_ACCOUNT_CREATE"]
}
```

### 10.5 Login / refresh principal creation

**File:** `src/main/java/com/hoandev/pinedrink/service/impl/AuthServiceImpl.java`

Thay doi:
- khong tu build authorities bang role nua
- dung `customUserDetailsService.buildPrincipal(account)`

Tac dung:
- login va refresh token deu tra access token co permission claim day du

### 10.6 Account controller migration

**File:** `src/main/java/com/hoandev/pinedrink/controller/AccountController.java`

Da migrate sang permission-based checks:

| Endpoint | Permission |
|----------|------------|
| `GET /api/v1/accounts` | `PERM_ACCOUNT_VIEW` |
| `GET /api/v1/accounts/{id}` | `PERM_ACCOUNT_VIEW` |
| `POST /api/v1/accounts` | `PERM_ACCOUNT_CREATE` |
| `PUT /api/v1/accounts/{id}` | `PERM_ACCOUNT_UPDATE` |
| `PATCH /api/v1/accounts/{id}/status` | `PERM_ACCOUNT_CHANGE_STATUS` |
| `POST /api/v1/accounts/{id}/reset-password` | `PERM_ACCOUNT_RESET_PASSWORD` |
| `GET /api/v1/accounts/{id}/roles` | `PERM_ACCOUNT_ROLE_VIEW` |
| `POST /api/v1/accounts/{id}/roles` | `PERM_ACCOUNT_ROLE_ASSIGN` |
| `DELETE /api/v1/accounts/{id}/roles/{assignmentId}` | `PERM_ACCOUNT_ROLE_REVOKE` |

---

## 11. Scope Enforcement Model

Module `Account` hien dang ket hop 2 lop:

### 11.1 Permission layer
- gate tai controller voi `@PreAuthorize(hasAuthority(...))`

### 11.2 Scope / business layer
- gate tai `AccountServiceImpl`
- cac helper dang dung:
  - `assertCanAccessAccount(...)`
  - `assertCanManageTargetScope(...)`
  - `assertCanAccessScope(...)`

Dieu nay tranh truong hop:
- user co permission dung
- nhung thao tac tren du lieu ngoai brand/branch duoc giao

---

## 12. File Inventory

### Database
- `src/main/resources/db/migration/V11__seed_permissions_and_role_permissions.sql`

### Security / Auth
- `src/main/java/com/hoandev/pinedrink/security/CustomUserDetailsService.java`
- `src/main/java/com/hoandev/pinedrink/security/UserPrincipal.java`
- `src/main/java/com/hoandev/pinedrink/security/JwtTokenProvider.java`
- `src/main/java/com/hoandev/pinedrink/security/JwtAuthFilter.java`
- `src/main/java/com/hoandev/pinedrink/service/impl/AuthServiceImpl.java`

### Authorization usage
- `src/main/java/com/hoandev/pinedrink/controller/AccountController.java`

### Repository
- `src/main/java/com/hoandev/pinedrink/repository/RolePermissionRepository.java`

---

## 13. Verification / Test Guidance

### 13.1 DB setup
- clear DB dev
- chay lai app de Flyway apply tu `V1` den `V11`

### 13.2 Token verification
- login bang `admin`
- decode JWT access token
- verify token co:
  - `roles`
  - `permissions`

### 13.3 API verification
- `admin` goi `GET /api/v1/accounts` -> pass
- `admin` goi `POST /api/v1/accounts` -> pass
- account khong co `PERM_ACCOUNT_CREATE` -> bi 403

### 13.4 Scope verification
- manager brand A co `PERM_ACCOUNT_VIEW`
- truy cap account brand B -> phai bi chan tai service layer

### 13.5 Build verification

Da verify compile thanh cong bang:

```bash
./mvnw -q -DskipTests compile
```

---

## 14. Risks and Notes

- Token hien co claim `permissions`, nhung backend van rebuild principal tu DB moi request; day la chu y thiet ke co chu dich
- Frontend hien tai nen tiep tuc render menu theo role de don gian hoa rollout
- Khi migrate them controller khac, can tach ro permission gate va scope gate, tranh nham permission la du
- `V11` da khong con la sample data; day la migration chinh thuc cho permission foundation

---

## 15. Next Steps

- Migrate `BranchController` sang permission-based checks
- Tao `PermissionCodes.java` de tranh hardcode chuoi `PERM_*`
- Mo rong permission sang `Order` va `Delivery`
- Them API doc role-permission matrix neu can backoffice UI sau nay
