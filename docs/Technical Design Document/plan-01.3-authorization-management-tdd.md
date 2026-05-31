# Technical Design Document - Authorization Management APIs

| Field | Value |
|---|---|
| Document ID | TDD-PINE-AUTHZ-MGMT-01.3 |
| Project | Pine Drink |
| Module | Authorization Management |
| Version | 1.0 |
| Status | Implemented |
| Last Updated | 2026-05-31 |

---

## 1. Mục Đích

Tài liệu này mô tả nhóm API quản lý role-permission cho backoffice UI.

Mục tiêu:
- Cho FE đọc danh sách role.
- Cho FE đọc danh sách permission master.
- Cho FE đọc matrix role -> permissions.
- Cho FE lưu permission set mới cho từng role.
- Cho BE invalidate Redis permission cache của user bị ảnh hưởng sau khi role-permission thay đổi.

---

## 2. Bối Cảnh

Trước phần này, FE chỉ toggle permission trên màn hình/memory:

```text
Bật/tắt checkbox -> chỉ đổi local state
Bấm lưu -> chưa đổi DB thật
Reload trang -> mất thay đổi
User khác -> chưa bị ảnh hưởng
```

Sau phần này, FE có thể lưu thay đổi thật vào DB:

```text
Bật/tắt checkbox
-> PUT role permissions
-> BE update ia_role_permission
-> BE invalidate Redis cache user có role đó
-> request sau dùng permission mới
```

---

## 3. API Design

Base path:

```http
/api/v1/authorization
```

### 3.1 Lấy danh sách roles

```http
GET /api/v1/authorization/roles
```

Required permission:

```text
PERM_ROLE_VIEW
```

Response data:

```json
[
  {
    "code": "ADMIN",
    "name": "Administrator",
    "description": "Full system access",
    "roleType": "SYSTEM",
    "status": "ACTIVE",
    "editable": false
  },
  {
    "code": "MANAGER",
    "name": "Manager",
    "description": "Branch management access",
    "roleType": "SYSTEM",
    "status": "ACTIVE",
    "editable": true
  }
]
```

`editable=false` nghĩa là FE nên hiển thị role nhưng không cho sửa permission.

---

### 3.2 Lấy danh sách permissions

```http
GET /api/v1/authorization/permissions
```

Required permission:

```text
PERM_PERMISSION_VIEW
```

Response data:

```json
[
  {
    "code": "BRANCH_VIEW",
    "name": "View branches",
    "module": "BRANCH",
    "description": "View branch detail and branch list by brand",
    "status": "ACTIVE"
  },
  {
    "code": "BRANCH_UPDATE",
    "name": "Update branches",
    "module": "BRANCH",
    "description": "Update branch information",
    "status": "ACTIVE"
  }
]
```

FE dùng danh sách này để render permission rows, group theo `module`.

---

### 3.3 Lấy role-permission matrix

```http
GET /api/v1/authorization/role-permissions/matrix
```

Required permission:

```text
PERM_ROLE_PERMISSION_VIEW
```

Response data:

```json
{
  "roles": [
    {
      "code": "ADMIN",
      "name": "Administrator",
      "description": "Full system access",
      "roleType": "SYSTEM",
      "status": "ACTIVE",
      "editable": false
    }
  ],
  "permissions": [
    {
      "code": "BRANCH_VIEW",
      "name": "View branches",
      "module": "BRANCH",
      "description": "View branch detail and branch list by brand",
      "status": "ACTIVE"
    }
  ],
  "matrix": {
    "ADMIN": ["BRANCH_VIEW", "BRANCH_UPDATE"],
    "MANAGER": ["BRANCH_VIEW"]
  }
}
```

FE dùng matrix để tick checkbox đúng trạng thái hiện tại trong DB.

---

### 3.4 Cập nhật permission set cho role

```http
PUT /api/v1/authorization/roles/{roleCode}/permissions
```

Required permission:

```text
PERM_ROLE_PERMISSION_UPDATE
```

Request body:

```json
{
  "permissions": [
    "BRANCH_VIEW",
    "BRANCH_UPDATE"
  ]
}
```

BE cũng chấp nhận dạng có prefix:

```json
{
  "permissions": [
    "PERM_BRANCH_VIEW",
    "PERM_BRANCH_UPDATE"
  ]
}
```

Response data:
- Trả lại `RolePermissionsMatrixResponse` mới sau khi update.

---

## 4. Update Semantics

API update dùng replace-set semantics.

Nghĩa là request body là danh sách permission cuối cùng mà role phải có.

Rule update `ia_role_permission`:

```text
Permission có trong request       -> status = ACTIVE
Permission không có trong request -> status = INACTIVE
Permission chưa từng có row       -> insert row mới status = ACTIVE
```

Ví dụ role `MANAGER` đang có:

```text
BRANCH_VIEW   ACTIVE
BRANCH_UPDATE ACTIVE
BRANCH_DELETE ACTIVE
```

FE save:

```json
{
  "permissions": [
    "BRANCH_VIEW",
    "BRANCH_UPDATE"
  ]
}
```

Kết quả DB:

```text
BRANCH_VIEW   ACTIVE
BRANCH_UPDATE ACTIVE
BRANCH_DELETE INACTIVE
```

Lý do dùng `ACTIVE`/`INACTIVE` thay vì delete row:
- Giữ row cũ để dễ bật lại.
- Hợp với schema hiện tại có cột `status`.
- Tránh mất lịch sử trạng thái cơ bản.
- Giữ unique pair `role_id + permission_id` ổn định.

Runtime permission query chỉ lấy row:

```text
rp.status = 'ACTIVE'
p.status = 'ACTIVE'
```

Nên row `INACTIVE` được coi như không có quyền.

---

## 5. ADMIN Protected Role

Hiện tại `ADMIN` là role cao nhất trong hệ thống.

Rule:

```text
ADMIN role không được sửa permission qua API runtime.
```

Nếu gọi:

```http
PUT /api/v1/authorization/roles/ADMIN/permissions
```

BE trả lỗi.

Lý do:
- Tránh admin tự xóa quyền critical.
- Tránh tự khóa hệ thống.
- Chưa có super-admin/recovery UI.

FE nên render `ADMIN` matrix read-only theo field:

```json
"editable": false
```

BE vẫn enforce rule này, không chỉ dựa vào FE.

---

## 6. Cache Invalidation

Khi cập nhật role-permission thành công, BE invalidate Redis permission cache của tất cả account đang active role đó.

Flow:

```text
PUT /roles/{roleCode}/permissions
-> update ia_role_permission
-> query active account ids by role
-> delete Redis keys auth:permissions:{accountId}
-> trả matrix mới
```

Redis key format:

```text
auth:permissions:{accountId}
```

Tác dụng:
- User có role bị sửa sẽ nhận permission mới ở request kế tiếp.
- `JwtAuthFilter` gặp Redis miss sẽ load lại permission từ DB.
- Không cần đợi JWT hết hạn.

Lưu ý FE:
- UI của user đang mở có thể stale đến khi gọi lại `/api/v1/auth/me/permissions`.
- BE authorization đã đúng ngay sau cache invalidation.
- Nếu FE gặp 403, nên reload permissions để sync UI.

---

## 7. Database Migration

Migration mới:

```text
src/main/resources/db/migration/V12__seed_authorization_management_permissions.sql
```

V12 chỉ seed thêm permissions cho authorization management:

```text
ROLE_VIEW
PERMISSION_VIEW
ROLE_PERMISSION_VIEW
ROLE_PERMISSION_UPDATE
```

V12 gán các permission này cho `ADMIN`.

V12 không đổi schema, không drop table, không phá migration cũ.

Nếu DB đã chạy tới V11, restart app để Flyway apply V12.

Sau khi V12 chạy, nên xóa permission cache của admin để nhận quyền mới ngay:

```bash
docker exec pine-drink-redis redis-cli -a redis123 DEL auth:permissions:00000000-0000-0000-0000-000000000030
```

---

## 8. Code Inventory

Controller:
- `src/main/java/com/hoandev/pinedrink/controller/AuthorizationManagementController.java`

Service:
- `src/main/java/com/hoandev/pinedrink/service/AuthorizationManagementService.java`
- `src/main/java/com/hoandev/pinedrink/service/impl/AuthorizationManagementServiceImpl.java`

DTOs:
- `src/main/java/com/hoandev/pinedrink/entity/dto/request/Authorization/UpdateRolePermissionsRequest.java`
- `src/main/java/com/hoandev/pinedrink/entity/dto/response/Authorization/RoleResponse.java`
- `src/main/java/com/hoandev/pinedrink/entity/dto/response/Authorization/PermissionResponse.java`
- `src/main/java/com/hoandev/pinedrink/entity/dto/response/Authorization/RolePermissionsMatrixResponse.java`

Repositories:
- `src/main/java/com/hoandev/pinedrink/repository/RoleRepository.java`
- `src/main/java/com/hoandev/pinedrink/repository/PermissionRepository.java`
- `src/main/java/com/hoandev/pinedrink/repository/RolePermissionRepository.java`
- `src/main/java/com/hoandev/pinedrink/repository/AccountRoleAssignmentRepository.java`

Migration:
- `src/main/resources/db/migration/V12__seed_authorization_management_permissions.sql`

---

## 9. Verification

Compile command:

```bash
./mvnw -q -DskipTests compile
```

Manual checks:
- Login admin.
- Clear admin permission cache after V12.
- Call `GET /api/v1/authorization/roles` -> 200.
- Call `GET /api/v1/authorization/permissions` -> 200.
- Call `GET /api/v1/authorization/role-permissions/matrix` -> 200.
- Call `PUT /api/v1/authorization/roles/MANAGER/permissions` -> DB updates and matrix returns new state.
- Call `PUT /api/v1/authorization/roles/ADMIN/permissions` -> blocked.
- Verify user with role `MANAGER` gets permission cache invalidated after save.

---

## 10. FE Integration Notes

Recommended FE flow:

```text
Open role-permission page
-> GET /authorization/role-permissions/matrix
-> render roles + permissions + checkbox state

Toggle checkbox
-> update local dirty state only

Click Save role
-> PUT /authorization/roles/{roleCode}/permissions
-> replace matrix with response data
```

FE should:
- Disable editing when `role.editable=false`.
- Send raw permission codes like `BRANCH_VIEW`.
- Optionally support sending `PERM_BRANCH_VIEW`; BE normalizes it.
- Reload `/api/v1/auth/me/permissions` after receiving 403 from business APIs.
