# Plan 12 — Permission Management Foundation

## Mục tiêu
Xây dựng nền tảng phân quyền theo `permission + scope` cho Pine Drink, nhưng triển khai theo hướng vừa đủ để không làm chậm các module business chính. Giai đoạn đầu tập trung vào backend authorization model, seed data, runtime check, và khả năng mở rộng từ mô hình `role-first` hiện tại.

## Hiện trạng

### Đã có
- `ia_role`, `ia_permission`, `ia_role_permission`, `ia_scope`, `ia_account_role_assignment` đã tồn tại trong schema tại `src/main/resources/db/migration/V2__create_ia_identity_access_schema.sql`
- Seed role mặc định trong `src/main/resources/db/migration/V10__init_data.sql`: `ADMIN`, `MANAGER`, `CUSTOMER`, `DELIVERY`
- Seed sample permission trong `src/main/resources/db/migration/V11__sample_data.sql`
- Entity/repository cơ bản đã có:
  - `entity/Permission.java`
  - `entity/RolePermission.java`
  - `repository/PermissionRepository.java`
  - `repository/RolePermissionRepository.java`
- Authorization hiện tại chủ yếu theo `role`, kết hợp `scope` trong một số service quan trọng như account management

### Chưa có hoặc chưa hoàn chỉnh
- Chưa load permission vào security context khi login/refresh token
- Chưa có helper/runtime guard check permission
- Chưa có constants tập trung cho permission codes
- Chưa có ma trận `role -> permission` chính thức cho từng module
- Chưa có API xem role có permission gì
- Chưa có API gán/thu hồi permission cho role
- Chưa có chiến lược migrate dần từ `@PreAuthorize(hasRole(...))` sang check permission

## Định hướng kiến trúc

### Nguyên tắc
- `Role` dùng để gom nhóm quyền
- `Permission` dùng để quyết định action cụ thể
- `Scope` dùng để giới hạn phạm vi dữ liệu được thao tác
- Backend mới là nơi quyết định quyền thật; frontend chỉ hiển thị module tương ứng

### Mô hình mục tiêu
- `role` trả lời câu hỏi: user thuộc nhóm vận hành nào
- `permission` trả lời câu hỏi: user được làm gì
- `scope` trả lời câu hỏi: user được làm ở phạm vi nào

Ví dụ:
- `ADMIN` + `SYSTEM` + `ACCOUNT_UPDATE` => có thể sửa account toàn hệ thống
- `MANAGER` + `BRAND` + `ORDER_VIEW` => chỉ xem đơn trong brand được gán
- `DELIVERY` + `BRANCH` + `DELIVERY_UPDATE_STATUS` => chỉ cập nhật đơn giao thuộc phạm vi được assign

## Phạm vi plan này

### Phase A — Foundation backend
- Chuẩn hóa permission codes
- Seed role-permission matrix chính thức
- Load permission cùng role khi xác thực
- Tạo helper check permission trong runtime

### Phase B — Module support
- API xem danh sách permission
- API xem permission theo role
- API gán/thu hồi permission cho role
- Cache permission lookup nếu cần

### Phase C — Gradual migration
- Chuyển các module nhạy cảm từ `role-only` sang `permission + scope`
- Bắt đầu với `Account`, `Order`, `Delivery`
- Giữ backward compatibility trong giai đoạn chuyển tiếp

## Permission modules đề xuất

### ACCOUNT
- `ACCOUNT_VIEW`
- `ACCOUNT_CREATE`
- `ACCOUNT_UPDATE`
- `ACCOUNT_CHANGE_STATUS`
- `ACCOUNT_RESET_PASSWORD`
- `ACCOUNT_ASSIGN_ROLE`

### ROLE
- `ROLE_VIEW`
- `ROLE_ASSIGN`
- `ROLE_PERMISSION_VIEW`
- `ROLE_PERMISSION_ASSIGN`

### CUSTOMER
- `CUSTOMER_VIEW`
- `CUSTOMER_UPDATE`

### MENU
- `MENU_VIEW`
- `MENU_CREATE`
- `MENU_UPDATE`
- `MENU_DELETE`

### ORDER
- `ORDER_VIEW`
- `ORDER_CREATE`
- `ORDER_UPDATE_STATUS`
- `ORDER_ASSIGN_DELIVERY`
- `ORDER_CANCEL`

### DELIVERY
- `DELIVERY_VIEW_ASSIGNED`
- `DELIVERY_UPDATE_STATUS`
- `DELIVERY_VIEW_HISTORY`

### INVENTORY
- `INVENTORY_VIEW`
- `INVENTORY_UPDATE`

### REPORT
- `REPORT_VIEW`

## Role -> Permission matrix giai đoạn đầu

### ADMIN
- Toàn bộ permission ở tất cả module
- Scope mặc định: `SYSTEM`

### MANAGER
- `ACCOUNT_VIEW`
- `CUSTOMER_VIEW`, `CUSTOMER_UPDATE`
- `MENU_VIEW`, `MENU_CREATE`, `MENU_UPDATE`
- `ORDER_VIEW`, `ORDER_UPDATE_STATUS`, `ORDER_ASSIGN_DELIVERY`
- `DELIVERY_VIEW_ASSIGNED`, `DELIVERY_VIEW_HISTORY`
- `INVENTORY_VIEW`, `INVENTORY_UPDATE`
- `REPORT_VIEW`
- Scope: `BRAND` hoặc `BRANCH`

### DELIVERY
- `ORDER_VIEW`
- `DELIVERY_VIEW_ASSIGNED`
- `DELIVERY_UPDATE_STATUS`
- `DELIVERY_VIEW_HISTORY`
- Scope: `BRANCH`

### CUSTOMER
- không cần dùng permission backoffice phức tạp
- có thể tiếp tục dùng self-service check riêng hoặc thêm nhóm permission client sau

## Files cần tạo/sửa

### Security / Auth
- `security/UserPrincipal.java` — bổ sung permission authorities nếu cần
- `security/CustomUserDetailsService.java` — load permission từ role assignments
- `service/impl/AuthServiceImpl.java` — load permission khi login/refresh
- `security/JwtTokenProvider.java` — cân nhắc có nhúng permission vào token hay không

### Authorization support
- `security/PermissionChecker.java` hoặc `security/AuthorizationService.java`
- `utils/PermissionCodes.java`
- `utils/RoleCodes.java` nếu muốn tách khỏi `Constants.java`

### Repositories
- `repository/RolePermissionRepository.java` — bổ sung query join fetch role/permission
- `repository/PermissionRepository.java` — hỗ trợ lookup theo module/status
- `repository/AccountRoleAssignmentRepository.java` — tận dụng join fetch role/scope hiện có

### API / Management
- `controller/RolePermissionController.java`
- `service/RolePermissionService.java`
- `service/impl/RolePermissionServiceImpl.java`

### DTO
- `entity/dto/response/Permission/PermissionResponse.java`
- `entity/dto/response/Permission/RolePermissionResponse.java`
- `entity/dto/request/Permission/AssignPermissionRequest.java`

### Migration / seed
- `db/migration/V12__seed_role_permissions.sql` hoặc file tên phù hợp tiếp theo

## Chi tiết implementation

### 1. Load authorities
Hiện tại `UserPrincipal` chủ yếu chứa `ROLE_*`. Giai đoạn mới nên load thêm `PERM_*` authority.

Ví dụ:

```java
List<GrantedAuthority> authorities = new ArrayList<>();
authorities.addAll(roleCodes.stream()
        .map(code -> new SimpleGrantedAuthority("ROLE_" + code))
        .toList());
authorities.addAll(permissionCodes.stream()
        .map(code -> new SimpleGrantedAuthority("PERM_" + code))
        .toList());
```

### 2. Permission lookup strategy
- Từ account -> active role assignments
- Từ role assignments -> roles
- Từ roles -> role_permissions
- Từ role_permissions -> permissions active

Nên có batch query để tránh N+1 nếu dùng trong list hoặc login flow nhiều nơi.

### 3. Runtime check strategy

#### Giai đoạn chuyển tiếp
- Controller vẫn có thể giữ `@PreAuthorize(hasRole(...))`
- Service thêm check permission ở các action nhạy cảm

#### Giai đoạn ổn định hơn
- Dùng `@PreAuthorize("hasAuthority('PERM_ACCOUNT_UPDATE')")`
- Hoặc custom helper:

```java
permissionChecker.require("ACCOUNT_UPDATE");
permissionChecker.requireInScope("ORDER_ASSIGN_DELIVERY", branchId);
```

### 4. Scope vẫn là bắt buộc
Permission không thay thế scope. Cần kết hợp:
- có permission đúng
- có scope đúng
- chỉ thao tác được trên dữ liệu trong phạm vi được gán

### 5. Seed data strategy
- Permission nên được seed bằng migration cố định
- Role-permission mapping cũng seed bằng SQL, không nhập tay trong code
- Dùng deterministic UUID giống các seed hiện tại

## API đề xuất cho permission management

### Read APIs
- `GET /api/v1/permissions` — danh sách permission
- `GET /api/v1/roles/{roleId}/permissions` — danh sách permission theo role

### Write APIs
- `POST /api/v1/roles/{roleId}/permissions` — gán permission cho role
- `DELETE /api/v1/roles/{roleId}/permissions/{permissionId}` — thu hồi permission khỏi role

### Phân quyền cho module này
- chỉ `SYSTEM ADMIN` hoặc account có `ROLE_PERMISSION_ASSIGN`

## Quy tắc nghiệp vụ

### Permission assignment
- Không tạo duplicate `(role_id, permission_id)`
- Chỉ gán permission đang `ACTIVE`
- Có thể khóa chỉnh sửa với một số role hệ thống cốt lõi nếu cần

### Backward compatibility
- Chưa xóa role check cũ ngay
- Migrate từng module một
- Module nào chưa migrate thì tiếp tục role-based

### Customer side
- Không cần áp backend permission management nặng ngay cho client/customer flows
- Ưu tiên permission cho backoffice trước

## Kế hoạch triển khai

### Phase 1 — Chuẩn hóa model
- Tạo `PermissionCodes.java`
- Bổ sung query role-permission
- Seed permission và role-permission matrix chính thức

### Phase 2 — Load permission vào auth
- Load permission tại login/refresh/user-details
- Bổ sung `PERM_*` authority vào security context
- Viết helper permission checker

### Phase 3 — Áp dụng vào module Account
- `ACCOUNT_VIEW`
- `ACCOUNT_UPDATE`
- `ACCOUNT_RESET_PASSWORD`
- `ACCOUNT_ASSIGN_ROLE`
- kết hợp với scope check đang có

### Phase 4 — Áp dụng vào Order/Delivery
- `ORDER_VIEW`
- `ORDER_ASSIGN_DELIVERY`
- `DELIVERY_UPDATE_STATUS`
- validate theo branch scope và assignment ownership

### Phase 5 — Permission management APIs
- Làm API đọc permission
- Làm API gán/thu hồi permission cho role
- Thêm audit log cho thay đổi quyền

## Test cases chính
- User có role đúng nhưng thiếu permission => bị chặn
- User có permission nhưng scope sai => bị chặn
- `MANAGER` brand A không xem/sửa dữ liệu brand B
- `DELIVERY` không truy cập module account/menu/inventory
- `ADMIN SYSTEM` có full access
- Permission inactive không được load vào authorities
- Role-permission duplicate bị chặn

## Kết quả mong đợi
Sau khi hoàn thành plan này, Pine Drink sẽ chuyển từ mô hình `role-first` sang nền tảng `permission + scope` bài bản hơn, nhưng vẫn giữ được tốc độ phát triển business feature. Đây là bước đệm tốt để mở rộng sang delivery workflow, backoffice nhiều vai trò, và kiểm soát quyền chi tiết hơn trong tương lai.
