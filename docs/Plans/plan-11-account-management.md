# Plan 11 — Account Management Module

## Mục tiêu
Hoàn thiện module quản lý account theo góc nhìn quản trị viên, bổ sung các chức năng còn thiếu bên cạnh phần auth/profile đã có sẵn. Module này tập trung vào quản lý danh sách tài khoản, chi tiết tài khoản, trạng thái account, và role assignment.

## Hiện trạng

### Đã có
- `controller/AuthController.java` — register, verify OTP, login, refresh token, logout, forgot/reset password, `GET /auth/me`
- `controller/ProfileController.java` — xem/cập nhật profile cá nhân, đổi mật khẩu, upload avatar
- `service/impl/AuthServiceImpl.java` — luồng đăng ký, xác thực OTP, token, refresh token, quên mật khẩu
- `service/impl/ProfileServiceImpl.java` — luồng self-service cho account hiện tại
- `entity/Account.java` + `repository/AccountRepository.java` — entity và repository cơ bản
- `security/CustomUserDetailsService.java` + `configuration/SecurityConfig.java` — JWT auth và RBAC nền tảng
- `db/migration/V2__create_ia_identity_access_schema.sql` — đã có bảng `ia_account`, `ia_role`, `ia_scope`, `ia_account_role_assignment`, `ia_refresh_token`, `ia_audit_log`

### Chưa có hoặc chưa hoàn chỉnh
- Chưa có `AccountController` riêng cho admin/manager
- Chưa có API danh sách account có filter + pagination
- Chưa có API xem chi tiết account bất kỳ
- Chưa có API admin tạo account nội bộ
- Chưa có API khóa / mở khóa / vô hiệu hóa account
- Chưa có API gán role, đổi role, xem role assignments
- Chưa có quy ước rõ ràng về phạm vi quản lý giữa `ADMIN` và `MANAGER`

## Phạm vi của module

### 1. Account Directory
- Danh sách account có phân trang
- Tìm kiếm theo username, email, phone, fullName
- Filter theo `status`, `role`, `brandId`
- Sắp xếp theo `createdAt`, `updatedAt`, `lastLoginAt`

### 2. Account Detail
- Xem thông tin account
- Xem customer profile liên kết nếu có
- Xem danh sách role assignment đang active
- Xem trạng thái hiện tại và thời điểm đăng nhập gần nhất

### 3. Account Administration
- Tạo account nội bộ cho admin/manager/delivery
- Cập nhật thông tin cơ bản của account
- Đổi trạng thái account: `ACTIVE`, `INACTIVE`, `LOCKED`
- Reset mật khẩu bởi admin

### 4. Role Management
- Gán role cho account
- Thu hồi role assignment
- Hỗ trợ scope `SYSTEM`, `BRAND`, `BRANCH`
- Chỉ lấy role assignment còn hiệu lực

## Đề xuất API

### Admin Account APIs
- `GET /api/v1/accounts` — danh sách account
- `GET /api/v1/accounts/{id}` — chi tiết account
- `POST /api/v1/accounts` — tạo account mới
- `PUT /api/v1/accounts/{id}` — cập nhật thông tin account
- `PATCH /api/v1/accounts/{id}/status` — đổi trạng thái account
- `POST /api/v1/accounts/{id}/reset-password` — reset mật khẩu bởi admin
- `GET /api/v1/accounts/{id}/roles` — xem role assignments
- `POST /api/v1/accounts/{id}/roles` — gán role
- `DELETE /api/v1/accounts/{id}/roles/{assignmentId}` — thu hồi role assignment

### Phân quyền đề xuất
- `ADMIN` — full access toàn hệ thống
- `MANAGER` — chỉ xem và quản lý account trong phạm vi brand/branch được gán
- `CUSTOMER` — không truy cập module này

## Files cần tạo/sửa

### Controllers
- `controller/AccountController.java`

### Services
- `service/AccountService.java`
- `service/impl/AccountServiceImpl.java`

### Repositories
- `repository/AccountRepository.java` — bổ sung search/filter query
- `repository/AccountRoleAssignmentRepository.java` — query lấy role assignments theo account
- `repository/RoleRepository.java` — lookup role theo code
- `repository/ScopeRepository.java` — lookup scope theo loại/phạm vi

### DTO Request
- `entity/dto/request/Account/CreateAccountRequest.java`
- `entity/dto/request/Account/UpdateAccountRequest.java`
- `entity/dto/request/Account/UpdateAccountStatusRequest.java`
- `entity/dto/request/Account/AssignRoleRequest.java`
- `entity/dto/request/Account/AdminResetPasswordRequest.java`
- `entity/dto/request/Account/AccountSearchRequest.java`

### DTO Response
- `entity/dto/response/Account/AccountListItemResponse.java`
- `entity/dto/response/Account/AccountDetailResponse.java`
- `entity/dto/response/Account/AccountRoleAssignmentResponse.java`

### Mapper / Spec
- `mapper/AccountManagementMapper.java`
- `specification/AccountSpecification.java` hoặc query trong repository

## Quy tắc nghiệp vụ

### Tạo account nội bộ
- Username phải unique
- Email unique nếu có truyền
- Phone unique nếu có truyền
- Password phải thỏa policy hiện tại
- Account tạo mới có thể mặc định `ACTIVE` hoặc `INACTIVE` tùy use case
- Có ít nhất 1 role assignment hợp lệ sau khi tạo account nội bộ

### Cập nhật account
- Không cho chỉnh `username` nếu muốn giữ ổn định định danh đăng nhập
- Không cho xóa role cuối cùng của account admin đang hoạt động nếu gây mất quyền quản trị hệ thống
- Không cho user tự đổi trạng thái qua module này

### Đổi trạng thái
- `LOCKED`: chặn đăng nhập ngay
- `INACTIVE`: coi như account chưa thể sử dụng
- `ACTIVE`: cho phép đăng nhập nếu các điều kiện khác hợp lệ
- Không cho khóa chính mình nếu request đến từ admin hiện tại

### Role assignment
- Một account có thể có nhiều role theo nhiều scope
- Không tạo duplicate `(account_id, role_id, scope_id)`
- Cho phép role assignment có `expiresAt`
- Chỉ load role active và chưa hết hạn vào security context

## Chi tiết implementation

### 1. AccountController

```java
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<BaseResponse<PageResponse<AccountListItemResponse>>> searchAccounts(...) {
        return ResponseEntity.ok(BaseResponse.success(accountService.searchAccounts(...)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<BaseResponse<AccountDetailResponse>> getAccountDetail(@PathVariable String id) {
        return ResponseEntity.ok(BaseResponse.success(accountService.getAccountDetail(id)));
    }
}
```

### 2. Search strategy
- Dùng `Pageable`
- Search keyword trên `username`, `email`, `phone`, `fullName`
- Filter động bằng `Specification` hoặc JPQL query riêng
- Trả về response tối ưu cho table list, không trả thừa dữ liệu

### 3. Account detail mapping
- Dữ liệu từ `Account`
- Join thêm `CustomerProfile` nếu account thuộc customer
- Join role assignments active
- Có thể trả `brandId` và danh sách scope đang được gán

### 4. Admin reset password
- Admin gửi password mới hoặc hệ thống tự generate password tạm
- Password phải encode bằng `PasswordEncoder`
- Nên revoke refresh token cũ sau khi reset mật khẩu để buộc đăng nhập lại

### 5. Status change và login flow
- Reuse logic kiểm tra status đang nằm trong `AuthServiceImpl`
- Chuẩn hóa status hợp lệ trong `Constants.java`
- Khi `LOCKED` hoặc `INACTIVE`, login phải trả đúng error code hiện có

## Query/Repository đề xuất

### AccountRepository
- `Page<Account> searchAccounts(...)`
- `Optional<Account> findByIdAndStatusNot(String id, String status)` nếu cần soft-delete sau này
- `boolean existsByEmailAndIdNot(String email, String id)`
- `boolean existsByUsernameAndIdNot(String username, String id)` nếu cho phép đổi username sau này

### AccountRoleAssignmentRepository
- `List<AccountRoleAssignment> findActiveAssignmentsByAccountId(String accountId, LocalDateTime now)`
- Query fetch role + scope để tránh N+1

## Response model gợi ý

### AccountListItemResponse
- `id`
- `username`
- `fullName`
- `email`
- `phone`
- `status`
- `lastLoginAt`
- `roles`

### AccountDetailResponse
- toàn bộ thông tin cơ bản của account
- profile bổ sung: `dateOfBirth`, `gender`, `avatarUrl`
- danh sách role assignments
- metadata: `createdAt`, `updatedAt`

## Kế hoạch triển khai

### Phase 1
- Tạo `AccountController`, `AccountService`
- Làm `GET /accounts` và `GET /accounts/{id}`
- Bổ sung DTO list/detail

### Phase 2
- Làm `POST /accounts`, `PUT /accounts/{id}`
- Làm `PATCH /accounts/{id}/status`
- Hoàn thiện validate duplicate username/email/phone

### Phase 3
- Làm role assignment APIs
- Làm admin reset password
- Revoke refresh tokens khi khóa account hoặc reset password

### Phase 4
- Bổ sung audit log cho create/update/status-change/assign-role
- Bổ sung test coverage

## Test cases chính
- Tạo account với username/email/phone trùng
- Tìm kiếm account theo keyword + filter status
- Manager không truy cập được account ngoài scope
- Không thể khóa chính mình
- Role assignment trùng bị chặn
- Account bị `LOCKED` không thể login
- Reset password xong refresh token cũ không còn dùng được

## Kết quả mong đợi
Sau khi hoàn thành plan này, hệ thống sẽ có module quản lý account hoàn chỉnh cho admin/manager, tách biệt rõ với phần auth/profile self-service hiện tại, đồng thời tận dụng được nền tảng identity-access đã xây dựng sẵn.
