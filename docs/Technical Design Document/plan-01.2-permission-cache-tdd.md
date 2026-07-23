# Technical Design Document - Permission Cache

| Field | Value |
|---|---|
| Document ID | TDD-PINE-PERM-CACHE-01.2 |
| Project | Pine Drink |
| Module | Permission Cache |
| Version | 1.0 |
| Status | Implemented |
| Last Updated | 2026-05-31 |

---

## 1. Mục Đích

Tài liệu này mô tả cách Pine Drink cache permission authorities cho user khi xác thực request bằng JWT.

Mục tiêu:
- Giảm số lần query DB trên mỗi request.
- Cho phép backend load permission mới hơn JWT claim.
- Giữ access token gọn, không cần nhúng permission vào token.
- Hỗ trợ invalidate cache khi role assignment thay đổi.

---

## 2. Bối Cảnh

Permission runtime được load qua:
- `src/main/java/com/hoandev/pinedrink/security/JwtAuthFilter.java`
- `src/main/java/com/hoandev/pinedrink/security/CustomUserDetailsService.java`
- `src/main/java/com/hoandev/pinedrink/service/PermissionCacheService.java`
- `src/main/java/com/hoandev/pinedrink/repository/RolePermissionRepository.java`

JWT hiện tại giữ identity + roles. Permissions được load riêng từ Redis/DB trong request authentication flow.

---

## 3. Runtime Flow

```text
Request có Bearer JWT
        |
        v
JwtAuthFilter validate token
        |
        v
CustomUserDetailsService.buildPrincipal(account, roleAuthorities)
        |
        v
PermissionCacheService.getPermissionAuthorities(accountId)
        |
        +--> Redis hit: trả cached PERM_*
        |
        +--> Redis miss: query DB qua RolePermissionRepository
                    |
                    v
              map permission code -> PERM_*
                    |
                    v
              lưu Redis với TTL 15 phút
                    |
                    v
              trả authorities
```

Sau đó `UserPrincipal` nhận authorities:

```text
ROLE_* + PERM_*
```

Controller dùng `@PreAuthorize(hasAuthority('PERM_*'))` để check action.

---

## 4. Thiết Kế Cache

File:
- `src/main/java/com/hoandev/pinedrink/service/PermissionCacheService.java`

Key format:

```text
auth:permissions:{accountId}
```

Ví dụ:

```text
auth:permissions:00000000-0000-0000-0000-000000000001
```

Value:

```text
List<String> authorities
```

Ví dụ:

```json
[
  "PERM_ACCOUNT_VIEW",
  "PERM_BRANCH_UPDATE"
]
```

TTL:

```text
15 phút
```

Lý do:
- Permission changes có thể có hiệu lực mà không cần chờ JWT hết hạn.
- Giảm tải DB trong request traffic bình thường.
- Nếu miss invalidation, stale permission window bị giới hạn bởi TTL.

---

## 5. Nguồn Query DB

File:
- `src/main/java/com/hoandev/pinedrink/repository/RolePermissionRepository.java`

Method:

```java
List<String> findActivePermissionCodesByAccountId(String accountId, LocalDateTime now);
```

Query filters:
- account role assignment thuộc account.
- assignment status là `ACTIVE`.
- assignment chưa hết hạn.
- role-permission status là `ACTIVE`.
- permission status là `ACTIVE`.

Output là raw permission code:

```text
ACCOUNT_VIEW
BRANCH_UPDATE
```

`PermissionCacheService` map sang Spring authority:

```text
PERM_ACCOUNT_VIEW
PERM_BRANCH_UPDATE
```

---

## 6. Quy Tắc Invalidate

Invalidation hiện đã triển khai:

File:
- `src/main/java/com/hoandev/pinedrink/service/impl/AccountServiceImpl.java`

Cases:
- Assign role cho account -> invalidate permission cache của target account.
- Revoke role khỏi account -> invalidate permission cache của target account.
- Create account kèm role -> invalidate permission cache của account vừa tạo.

Service methods:

```java
permissionCacheService.invalidateUserCache(accountId);
permissionCacheService.invalidateUserCaches(accountIds);
```

Invalidation cần có trong tương lai:
- Thêm permission vào role -> invalidate tất cả user đang được gán role đó.
- Gỡ permission khỏi role -> invalidate tất cả user đang được gán role đó.
- Disable permission -> invalidate user đang nhận permission đó.
- Disable role-permission row -> invalidate user đang được gán role đó.
- Đổi assignment expiry/status ngoài `AccountService` -> invalidate user bị ảnh hưởng.

---

## 7. Consistency Model

Permission cache dùng cache-aside pattern.

```text
Read path:
Redis -> DB fallback -> Redis set

Write path:
DB update -> Redis delete
```

Expected consistency:
- Sau khi assign/revoke role qua `AccountService`, permission changes có hiệu lực ở request kế tiếp.
- Nếu miss invalidation, permission cũ có thể sống tối đa 15 phút.
- JWT role claim có thể stale tới khi token hết hạn, nên business module nên ưu tiên `PERM_*` checks thay vì role checks.

---

## 8. Failure Behavior

Code hiện tại đang giả định Redis operations chạy ổn.

Potential failure cases:
- Redis unavailable khi gọi `getPermissionAuthorities()`.
- Redis serialization mismatch cho `List<String>`.
- Cached value bị corrupt hoặc sai type.

Recommended hardening:
- Catch Redis read/write exceptions và fallback về DB.
- Log cache failures nhưng không block authentication nếu DB còn available.
- Giữ cached value type ổn định là `List<String>`.
- Cân nhắc thêm metrics cho hit/miss/error.

---

## 9. Security Notes

- Không trust permission state từ frontend.
- Không trust JWT cho permission checks nếu backend có thể load permission động.
- Cache chỉ lưu authorities, không lưu secret.
- Permission cache key theo account, không theo role.
- Cache invalidation phải là một phần của mọi role-permission write API trong tương lai.

---

## 10. Verification

Compile command:

```bash
./mvnw -q -DskipTests compile
```

Manual test cases:
- Login admin -> gọi protected permission endpoint -> pass.
- Revoke role khỏi account -> request kế tiếp fail sau cache invalidation.
- Assign role cho account -> request kế tiếp có permissions mới.
- Xóa Redis key `auth:permissions:{accountId}` -> request kế tiếp repopulate cache từ DB.
- Dùng account không có permission -> controller trả 403.

---

## 11. Next Steps

- Thêm Redis fallback-to-DB hardening trong `PermissionCacheService`.
- Thêm role-permission management API với bulk invalidation.
- Thêm integration tests cho cache hit/miss/invalidation.
- Thêm metrics: cache hit, miss, delete, DB fallback, Redis error.
- Cân nhắc TTL ngắn hơn cho môi trường yêu cầu bảo mật cao.
