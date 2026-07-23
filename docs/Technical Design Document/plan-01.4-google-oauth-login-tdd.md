# Technical Design Document - Google OAuth Login

| **Field** | **Value** |
|-----------|-----------|
| **Document ID** | TDD-PINE-AUTH-004 |
| **Project** | Pine Drink - Hệ thống order đồ uống online |
| **Module** | Authentication |
| **Version** | 1.1 |
| **Status** | Draft |
| **Author** | Pine Drink Dev Team |
| **Last Updated** | 2026-06-03 |

---

## 1. Tiêu Đề

Đăng nhập bằng Google cho Pine Drink Platform.

---

## 2. Tổng Quan

Tài liệu này mô tả thiết kế tính năng đăng nhập bằng Google cho Pine Drink. Hệ thống hiện tại dùng Spring Boot REST API, JWT access token, refresh token lưu trong database, phân quyền bằng role/scope. Google login sẽ được tích hợp theo hướng API-first: frontend lấy Google ID token, backend xác thực token với Google, sau đó phát hành JWT nội bộ của Pine Drink.

Giải pháp giữ nguyên nền tảng bảo mật hiện tại:

- Backend vẫn là stateless REST API.
- Access token và refresh token vẫn do Pine Drink phát hành.
- Role `CUSTOMER`, scope `SYSTEM`, `CustomerProfile` vẫn được tạo theo chuẩn hiện có.
- Frontend không phụ thuộc Spring Security session.

---

## 3. Mục Tiêu

- Cho phép khách hàng đăng nhập nhanh bằng tài khoản Google.
- Giảm ma sát so với đăng ký bằng OTP/password.
- Giữ response login giống endpoint username/password hiện tại.
- Đảm bảo backend xác thực Google token hợp lệ trước khi phát hành JWT nội bộ.
- Tạo nền tảng mở rộng sang Facebook, Apple hoặc provider khác.

---

## 4. Phạm Vi

### Trong Phạm Vi

- Thêm endpoint `POST /api/v1/auth/google`.
- Xác thực Google ID token bằng Google client library.
- Tạo account mới nếu email Google chưa tồn tại.
- Đăng nhập account hiện có nếu email đã tồn tại.
- Gán role `CUSTOMER`, scope `SYSTEM`, tạo `CustomerProfile` cho account mới.
- Trả `LoginResponse` gồm access token, refresh token, thông tin account.
- Thêm cấu hình `GOOGLE_CLIENT_ID`.

### Ngoài Phạm Vi

- Redirect OAuth flow `/oauth2/authorization/google` của Spring Security.
- Lưu Google access token hoặc Google refresh token.
- Gọi Google API khác như Calendar, Drive, Contacts.
- Link/unlink nhiều social provider trong profile.
- Facebook/Apple login.

---

## 5. Đối Tượng

| Đối Tượng | Vai Trò |
|-----------|---------|
| Backend Developer | Triển khai Google login API |
| Frontend Developer | Tích hợp nút Continue with Google |
| QA / Tester | Viết test case Google login |
| DevOps | Cấu hình Google Client ID và biến môi trường |
| Technical Leader | Review thiết kế bảo mật |

---

## 6. Bối Cảnh

### 6.1 Hiện Trạng Hệ Thống

Pine Drink hiện có auth module tại `AuthController`, `AuthServiceImpl`, `SecurityConfig`. Login truyền thống dùng `POST /api/v1/auth/login` với username/email và password. Backend tạo JWT access token bằng `JwtTokenProvider`, tạo refresh token, hash và lưu vào bảng `ia_refresh_token`.

Bảng `ia_account` hiện bắt buộc `username`, `password`, `full_name`. Email unique nhưng nullable. Account mới từ register OTP có status `INACTIVE`, sau verify OTP mới thành `ACTIVE` và được gán role `CUSTOMER`.

### 6.2 Vấn Đề

Nếu dùng Spring OAuth redirect flow trực tiếp, backend cần callback/session flow, không hợp với frontend SPA/mobile hiện tại. Hệ thống đang theo REST stateless nên nên dùng Google ID token verification.

### 6.3 Giải Pháp

Frontend dùng Google Identity Services để lấy `id_token`. Backend nhận token và kiểm tra:

- Chữ ký hợp lệ.
- `aud` khớp `GOOGLE_CLIENT_ID`.
- `iss` là `accounts.google.com` hoặc `https://accounts.google.com`.
- Token chưa hết hạn.
- `email_verified=true`.

Nếu token hợp lệ, backend tạo hoặc lấy account, kiểm tra trạng thái, phát hành JWT nội bộ.

---

## 7. Yêu Cầu

### 7.1 Yêu Cầu Chức Năng

| # | Yêu Cầu | Mô Tả |
|---|---------|-------|
| FR1 | Google login endpoint | API nhận Google ID token và trả `LoginResponse` |
| FR2 | Token verification | Backend xác thực ID token với Google và client ID cấu hình |
| FR3 | Auto-provision account | Tạo account `CUSTOMER` nếu email chưa tồn tại |
| FR4 | Existing account login | Nếu email đã tồn tại, đăng nhập account đó |
| FR5 | Account status check | Chặn account `LOCKED`, `INACTIVE`, `DELETED` theo rule hiện tại |
| FR6 | Role assignment | Account mới được gán role `CUSTOMER` với scope `SYSTEM` |
| FR7 | Profile creation | Account mới được tạo `CustomerProfile` |
| FR8 | JWT issuance | Response gồm access token, refresh token, token type, expiry, account |

### 7.2 Yêu Cầu Phi Chức Năng

| # | Yêu Cầu | Mô Tả |
|---|---------|-------|
| NFR1 | Stateless | Không tạo Spring session |
| NFR2 | Latency | Verify Google token và login dưới 1 giây trong điều kiện bình thường |
| NFR3 | Compatibility | Không phá vỡ login password/OTP hiện tại |
| NFR4 | Observability | Log thành công/thất bại nhưng không ghi raw ID token |
| NFR5 | Security | Chỉ chấp nhận email đã verify từ Google |

### 7.3 Yêu Cầu Bảo Mật

| # | Yêu Cầu | Giá Trị |
|---|---------|---------|
| SR1 | Google audience | Phải khớp `${GOOGLE_CLIENT_ID}` |
| SR2 | Email verification | `email_verified=true` |
| SR3 | Token storage | Không lưu Google ID token |
| SR4 | Secret exposure | Không commit `.env`, client secret, raw token |
| SR5 | Account status | Gọi validate status trước khi phát JWT |
| SR6 | Rate limit | Endpoint Google login dùng nhóm limit login |

---

## 8. Thiết Kế API

### 8.1 Google Login

```http
POST /api/v1/auth/google
Content-Type: application/json
```

Request:

```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIs..."
}
```

Response thành công:

```json
{
  "success": true,
  "message": "Google login successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "account": {
      "id": "...",
      "username": "john_doe",
      "fullName": "John Doe",
      "email": "john.doe@gmail.com",
      "phone": null,
      "avatarUrl": "https://lh3.googleusercontent.com/...",
      "status": "ACTIVE",
      "lastLoginAt": "2026-06-03T10:00:00"
    }
  }
}
```

### 8.2 Error Cases

| Trường Hợp | HTTP | Error Code | Mô Tả |
|------------|------|------------|-------|
| Thiếu `idToken` | 400 | `AUTH_GOOGLE_001` | Request không có token |
| ID token không hợp lệ | 401 | `AUTH_GOOGLE_002` | Token sai chữ ký, hết hạn hoặc audience sai |
| Email chưa verify | 401 | `AUTH_GOOGLE_003` | Google email chưa được xác thực |
| Email đã dùng bởi provider khác | 409 | `AUTH_GOOGLE_004` | Account đã có authProvider != LOCAL |
| Google sub không khớp | 401 | `AUTH_GOOGLE_005` | ProviderId không khớp với Google sub |
| Account bị khóa | 403 | `AUTH_005` | Account `LOCKED` |
| Account chưa active | 403 | `AUTH_006` | Account `INACTIVE` |
| Local password đã tồn tại | 400 | `AUTH_025` | Gọi `set-password` khi đã có mật khẩu |
| Local password chưa set | 400 | `AUTH_026` | Gọi `change-password` khi chưa có mật khẩu |
| Thiếu role/scope seed | 500 | `ROLE_NOT_FOUND`/`SCOPE_NOT_FOUND` | Seed data chưa đầy đủ |

---

## 9. Thiết Kế Data Model

### 9.1 Phương Án Tối Thiểu

Không đổi schema. Account Google mới sẽ có:

| Column | Giá Trị |
|--------|---------|
| `username` | Sinh từ email prefix, thêm suffix nếu trùng |
| `password` | BCrypt của random UUID/password vô nghĩa |
| `full_name` | Google `name` hoặc email prefix |
| `email` | Google email lowercase |
| `avatar_url` | Google `picture` |
| `status` | `ACTIVE` |

Ưu điểm: nhanh, ít migration, hợp với schema hiện tại.

Nhược điểm: database chưa biết account được tạo từ provider nào.

### 9.2 Phương Án Chuẩn Hóa Khuyến Nghị

Thêm column vào `ia_account`:

Migration V17:

```sql
ALTER TABLE ia_account
    ADD COLUMN auth_provider VARCHAR(30) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN provider_id VARCHAR(150) NULL,
    ADD UNIQUE KEY uk_ia_account_provider (auth_provider, provider_id);
```

Migration V18:

```sql
ALTER TABLE ia_account
    ADD COLUMN has_local_password BOOLEAN NOT NULL DEFAULT TRUE AFTER provider_id;
```

| Column | Local Account | Google Account |
|--------|---------------|----------------|
| `auth_provider` | `LOCAL` | `GOOGLE` |
| `provider_id` | null | Google `sub` |
| `has_local_password` | `true` | `false` (cho đến khi user tự set) |
| `password` | BCrypt password | Random encoded placeholder |

Giá trị `has_local_password` theo từng loại account:

| Account Type | Tình Trạng | `has_local_password` |
|-------------|------------|---------------------|
| LOCAL (đăng ký bằng password) | Luôn có mật khẩu thật | `true` |
| GOOGLE mới tạo | Chưa set mật khẩu lần nào | `false` |
| GOOGLE đã set mật khẩu | Có mật khẩu thật | `true` |

Cơ chế này cho phép FE phân biệt account `LOCAL` và `GOOGLE`, đồng thời biết account Google đã set mật khẩu local hay chưa để hiển thị flow `Đổi mật khẩu` / `Thiết lập mật khẩu` tương ứng.

---

## 10. Thiết Kế Backend

### 10.1 Dependency

Thêm Google API client vào `pom.xml`:

```xml
<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
    <version>2.7.0</version>
</dependency>
```

### 10.2 Configuration

Thêm vào `application.yaml`:

```yaml
app:
  oauth2:
    google:
      client-id: ${GOOGLE_CLIENT_ID:}
```

Biến môi trường local:

```properties
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
```

### 10.3 SecurityConfig

Thêm endpoint public:

```java
"/api/v1/auth/google"
```

Endpoint này vẫn nên đi qua `RateLimitFilter` để tránh spam token verification.

### 10.4 DTO

Package đề xuất: `com.hoandev.pinedrink.entity.dto.request.Auth`.

```java
public class GoogleLoginRequest {
    @NotBlank(message = "Google ID token is required")
    private String idToken;
}
```

### 10.5 Controller

Thêm vào `AuthController`:

```java
@PostMapping("/google")
public ResponseEntity<BaseResponse<LoginResponse>> googleLogin(
        @RequestBody @Valid GoogleLoginRequest request
) {
    LoginResponse response = authService.googleLogin(request.getIdToken());
    return ResponseEntity.ok(
            BaseResponse.success(response, "Google login successfully")
    );
}
```

### 10.6 Service Interface

Thêm vào `AuthService`:

```java
LoginResponse googleLogin(String idToken);
```

### 10.7 Service Flow

Sơ đồ sequence dưới đây mô tả chi tiết luồng xử lý khi frontend gửi ID token:

```text
FRONTEND                    BACKEND                      GOOGLE           DATABASE
    |                           |                           |                |
    | POST /api/v1/auth/google  |                           |                |
    | { idToken }               |                           |                |
    |-------------------------->|                           |                |
    |                           | verifyIdToken(idToken)    |                |
    |                           |-------------------------->|                |
    |                           |    payload | null         |                |
    |                           |<--------------------------|                |
    |                           |                           |                |
    |    [Token invalid]        |                           |                |
    |<-- 401 AUTH_GOOGLE_002    |                           |                |
    |                           |                           |                |
    |    [Email not verified]   |                           |                |
    |<-- 401 AUTH_GOOGLE_003    |                           |                |
    |                           |                           |                |
    |    [Token hợp lệ]         |                           |                |
    |                           | findByProvider(GOOGLE,sub)|                |
    |                           |------------------------------------------>|
    |                           |   Optional<Account>      |                |
    |                           |<------------------------------------------|
    |                           |                           |                |
    |    [Có Google account]    | validateAccountStatus()   |                |
    |                           |------------------------------------------>|
    |    [Chưa có]              |                           |                |
    |                           | findByEmail(email)        |                |
    |                           |------------------------------------------>|
    |                           |   Optional<Account>      |                |
    |                           |<------------------------------------------|
    |                           |                           |                |
    |    [Email tồn tại]        | validateGoogleLink()     |                |
    |    [AuthProvider GOOGLE]  | check sub mismatch        |                |
    |    [AuthProvider LOCAL]   | cho link                  |                |
    |    [Provider khác]        | throw AUTH_GOOGLE_004     |                |
    |<-- 409/401                |                           |                |
    |                           |                           |                |
    |    [Email mới]            | createGoogleAccount()    |                |
    |                           | assignCustomerRole()     |                |
    |                           | createCustomerProfile()   |                |
    |                           |------------------------------------------>|
    |                           |                           |                |
    |                           | Sau khi xác định account: |                |
    |                           | set auth_provider (nếu chưa có)          |
    |                           | set provider_id (nếu chưa có)             |
    |                           | set avatar (nếu null)     |                |
    |                           |                           |                |
    |                           | invalidatePermissionCache |                |
    |                           | buildPrincipal()          |                |
    |                           | generateAccessToken()     |                |
    |                           | generateRefreshToken()    |                |
    |                           | saveRefreshToken()        |                |
    |                           | update lastLoginAt        |                |
    |<-- 200 LoginResponse      |                           |                |
```

---

## 11. Thiết Kế Account Provisioning

### 11.1 Tạo Account

Khi email chưa tồn tại:

- Tạo `Account` mới.
- `username` sinh từ email prefix.
- `password` là BCrypt của random UUID.
- `fullName` lấy từ Google `name`.
- `email` lowercase.
- `avatarUrl` lấy từ Google `picture`.
- `status=ACTIVE`.
- `hasLocalPassword=false`.

### 11.2 Gán Role

Gán role mặc định:

- Role code: `CUSTOMER`.
- Scope: `SYSTEM` với `branch_id=null` (dùng `findByScopeTypeAndBranchIdIsNull`).
- Status: `ACTIVE`.
- Assigned at: `LocalDateTime.now()`.

### 11.3 Tạo CustomerProfile

Tạo `CustomerProfile`:

- `fullName` = account fullName.
- `email` = account email.
- `phone` = null.
- `customerCode` = `codeGenerator.generate("KH")`.
- `status=ACTIVE`.

### 11.4 Sinh Username

Rule:

```text
base = phần trước @ của email
normalize: lowercase, giữ a-z, 0-9, underscore
nếu length < 3 -> thêm random digits
nếu exists -> thêm _ + 6 random chars
```

Ví dụ:

```text
john.doe@gmail.com -> john_doe
john_doe exists -> john_doe_a1b2c3
```

### 11.5 hasLocalPassword Và Set Password

`has_local_password` là cờ quan trọng để phân biệt account có mật khẩu local thật sự hay không.

**Ý nghĩa tồn tại của cờ này:**
- `password` trong `ia_account` luôn `NOT NULL`, kể cả Google account (lưu placeholder).
- Không thể dùng `password != null` để biết user có mật khẩu thật.
- `auth_provider` không đủ, vì Google account có thể set mật khẩu local sau đó.

**Quy tắc nghiệp vụ:**

| API | Điều Kiện | Hành Vi |
|-----|-----------|---------|
| `PUT /api/v1/profile/password` | `hasLocalPassword=true` | Đổi mật khẩu, cần `currentPassword` |
| `POST /api/v1/profile/set-password` | `hasLocalPassword=false` | Thiết lập mật khẩu lần đầu, không cần `currentPassword` |

**Điểm bảo mật cần lưu ý:**

| Tình Huống | Xử Lý |
|-----------|-------|
| Google account gọi `changePassword` | Throw `AUTH_026` (local password is not set) |
| Local account gọi `setPassword` | Throw `AUTH_025` (local password is already set) |
| set password lần đầu từ Google | Sau khi gọi thành công, `hasLocalPassword` đổi thành `true` |
| change password trên account local | Giữ nguyên `hasLocalPassword=true` |

---

## 12. Luồng Chi Tiết & Quyết Định Nghiệp Vụ

### 12.1 Decision Tree - Xác Định Account

Khi backend nhận được ID token hợp lệ, luồng quyết định account như sau:

```text
                         ID TOKEN HỢP LỆ
                       email_verified = true
                               |
                     Normalize email: lowercase
                               |
              ___________________________________
             |                                  |
             |      findByAuthProviderAnd        |
             |      ProviderId(GOOGLE, sub)      |
             |                                  |
         [FOUND]                            [NOT FOUND]
             |                                  |
             |                           findByEmail(email)
             |                                  |
             |                     ________________|________________
             |                    |                |                |
             |                [FOUND]          [NOT FOUND]      [ERROR]
             |                    |                |                |
             |            validateGoogleLink   TẠO MỚI       400 BAD
             |                    |                |
             |          __________|__________      |
             |         |         |          |      |
             |    GOOGLE     LOCAL      KHÁC      |
             |    cùng sub   /null     provider   |
             |         |         |          |     |
             |      [OK]      [OK]     [CONFLICT]  |
             |         |         |     AUTH_...004 |
             |         |         |          |     |
             |         |    LINK GOOGLE     |     |
             |         |    (set provider)  |     |
             |         |         |          |     |
             └─────────┴─────────┴──────────┴─────┘
                               |
                    VALIDATE ACCOUNT STATUS
                    (ACTIVE mới cho qua)
                               |
                    UPDATE/LINK GOOGLE INFO
                    (auth_provider, provider_id, avatar)
                               |
                    BUILD PRINCIPAL + JWT
                               |
                    LOGIN RESPONSE
```

### 12.2 Bảy Trường Hợp Nghiệp Vụ

| # | Tình Huống | Kết Quả | Giải Thích |
|---|------------|---------|------------|
| 1 | `sub` mới, `email` mới | Tạo account Google mới | Người dùng chưa từng có account ở Pine Drink |
| 2 | `sub` mới, `email` trùng account `LOCAL` | Link Google vào account local | User trước đó dùng password, giờ login bằng Google |
| 3 | `sub` mới, `email` trùng account `GOOGLE` cùng `sub` | Login account đó | Bản ghi có sẵn, không thay đổi |
| 4 | `sub` mới, `email` trùng account `GOOGLE` khác `sub` | Throw `AUTH_GOOGLE_005` | Google sub không khớp, không cho login |
| 5 | `sub` mới, `email` trùng account provider khác (FACEBOOK) | Throw `AUTH_GOOGLE_004` | Email đã dùng bởi provider khác, không thể link |
| 6 | `sub` có sẵn, account `LOCKED`/`INACTIVE` | Throw `AUTH_005`/`AUTH_006` | Tài khoản bị khóa hoặc chưa active |
| 7 | `sub` có sẵn, account `ACTIVE` | Login bình thường | Luồng happy case, update lastLogin |

### 12.3 Quy Tắc Link Account

**Có thể link Google vào account khi:**

- `auth_provider` là `null` hoặc blank (dữ liệu cũ chưa có provider).
- `auth_provider` là `LOCAL` (account đăng ký bằng username/password).
- `auth_provider` là `GOOGLE` và `provider_id` khớp với `sub` (không thay đổi).

**Không thể link Google vào account khi:**

- `auth_provider` là `GOOGLE` và `provider_id` khác `sub`.
- `auth_provider` không phải `LOCAL`, không phải `null` (ví dụ `FACEBOOK`).

**Những gì thay đổi trên account khi link:**

- `auth_provider` = `GOOGLE` (nếu chưa có).
- `provider_id` = Google `sub` (nếu chưa có).
- `avatar_url` = Google `picture` (nếu chưa có avatar).

### 12.4 Thứ Tự Validate Quan Trọng

Thứ tự các bước kiểm tra trong `googleLogin()` rất quan trọng:

```text
  1. Verify token với Google            [fail → AUTH_GOOGLE_002/003]
  2. Xác định account (findOrCreate)     [fail → AUTH_GOOGLE_004/005]
  3. Kiểm tra trạng thái account        [fail → AUTH_005/006]
  4. Link/provider update               [thay đổi DB]
  5. Invalidate permission cache
  6. Build principal & JWT
```

Lý do của thứ tự này:

- **Bước 3 trước bước 4**: Nếu account bị `LOCKED`, DB sẽ được rollback nhờ `@Transactional`, tránh ghi thông tin provider vào account không hợp lệ.
- **Bước 4 chỉ thay đổi DB sau khi chắc chắn được phép login**: Đây là điểm bảo mật quan trọng, tránh "cướp" account bị khóa.

### 12.5 Các Điểm Cần Lưu Ý Khi Review Code

| File | Dòng | Điểm Quan Trọng |
|------|------|-----------------|
| `AuthServiceImpl.googleLogin()` | - | Luồng đã tách `findOrCreateGoogleAccount()` riêng |
| `AuthServiceImpl.findOrCreateGoogleAccount()` | - | Ưu tiên `providerId` trước, fallback `email` sau |
| `AuthServiceImpl.validateGoogleLink()` | - | Chặn provider conflict (FACEBOOK, sub mismatch) |
| `AuthServiceImpl.canLinkGoogle()` | - | Chỉ cho phép link với `LOCAL` hoặc null |
| `ScopeRepository.findByScopeTypeAndBranchIdIsNull()` | - | Tránh null JPA query bug |
| `AccountRepository.findByAuthProviderAndProviderId()` | - | Tìm nhanh account tồn tại của Google |
| `AuthMapper.toLoginResponse()` | - | Tái sử dụng response format, giữ đồng bộ |
| `Constants.AUTH_PROVIDER_GOOGLE` | - | Dùng constant, không hardcode |
| `GlobalExceptionHandler` | `AUTH_GOOGLE_004` | Phải trả 409 CONFLICT để frontend xử lý đúng |
| `ProfileServiceImpl.changePassword()` | - | Kiểm tra `hasLocalPassword=true` trước, nếu không throw `AUTH_026` |
| `ProfileServiceImpl.setPassword()` | - | Kiểm tra `hasLocalPassword=false` trước, nếu không throw `AUTH_025` |
| `AuthServiceImpl.createGoogleAccount()` | - | Set `hasLocalPassword=false` khi tạo account Google |
| `AccountResponse` | `authProvider`, `hasLocalPassword` | Trả 2 field này để FE render đúng UI |
| `ErrorCode.AUTH_025` | - | "Local password is already set" |
| `ErrorCode.AUTH_026` | - | "Local password is not set" |

### 12.6 Xử Lý Race Condition

| Rủi Ro | Mức Độ | Giải Pháp Hiện Tại | Giải Pháp Chuẩn |
|--------|--------|--------------------|-----------------|
| `generateUniqueUsername()` - hai request cùng lúc sinh username trùng | Thấp | DB unique constraint bắt | Bắt `DataIntegrityViolationException` + retry |
| `findOrCreateGoogleAccount()` - hai request Google cùng lúc với `sub` mới | Trung bình | Transaction + unique constraint | Có thể thêm `SELECT ... FOR UPDATE` |
| `validateAccountStatus()` + `setProviderId()` bị rollback nếu throw | Thấp | `@Transactional` rollback tự động | Không cần thay đổi |
| Link provider vào account bị LOCKED rồi rollback | Thấp | Đã đảo validateStatus trước setProviderId | OK |

### 12.7 Luồng Extension Cho Provider Khác

Khi thêm Facebook/Apple login trong tương lai:

```text
AuthService
  ├── login()              -> username/password
  ├── googleLogin()        -> GOOGLE
  ├── facebookLogin()      -> FACEBOOK   (thêm sau)
  ├── appleLogin()         -> APPLE      (thêm sau)
  └── ...

Mỗi provider cần:
  1. TokenVerifier riêng (vd: FacebookTokenVerifier)
  2. findOrCreate theo (provider, providerId)
  3. validateLink với canLink rules
  4. Tái sử dụng assignCustomerRole, createCustomerProfile
```

---

## 13. Hướng Dẫn Frontend

Frontend dùng Google Identity Services:

```html
<script src="https://accounts.google.com/gsi/client" async defer></script>
```

Sau khi user chọn Google account, frontend nhận credential:

```javascript
async function handleGoogleCredential(response) {
  const res = await fetch('/api/v1/auth/google', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ idToken: response.credential })
  });

  const body = await res.json();
  localStorage.setItem('accessToken', body.data.accessToken);
  localStorage.setItem('refreshToken', body.data.refreshToken);
}
```

Frontend không cần biết Google access token. Frontend chỉ lưu Pine Drink JWT như login thường.

### 13.1 Xử Lý Profile Response

Response từ `GET /api/v1/profile` có 2 field mới:

```json
{
  "authProvider": "GOOGLE",
  "hasLocalPassword": false
}
```

FE render button dựa vào các field này:

```javascript
if (profile.authProvider === 'GOOGLE' && !profile.hasLocalPassword) {
  // Hiển thị nút "Thiết lập mật khẩu"
  // → POST /api/v1/profile/set-password
}

if (profile.hasLocalPassword) {
  // Hiển thị nút "Đổi mật khẩu"
  // → PUT /api/v1/profile/password (cần currentPassword)
}
```

---

## 14. Cấu Hình Google Cloud

1. Tạo project trong Google Cloud Console.
2. Vào APIs & Services -> OAuth consent screen.
3. Cấu hình app name, support email, developer contact.
4. Vào Credentials -> Create Credentials -> OAuth client ID.
5. Chọn Web application.
6. Thêm Authorized JavaScript origins:

```text
http://localhost:4200
http://localhost:5173
http://localhost:3000
```

7. Copy Client ID vào env:

```properties
GOOGLE_CLIENT_ID=xxxx.apps.googleusercontent.com
```

Ghi chú: Flow ID token từ frontend sang backend không cần redirect URI nếu dùng Google Identity Services button/one tap.

---

## 15. Chiến Lược Test

### 15.1 Unit Test

| Test | Expected |
|------|----------|
| Google payload hợp lệ, email mới | Tạo account, role, profile, trả token |
| Google payload hợp lệ, email tồn tại (LOCAL) | Link Google, login thành công |
| Google payload hợp lệ, email tồn tại (GOOGLE cùng sub) | Login account đó |
| Google payload hợp lệ, email tồn tại (GOOGLE khác sub) | Throw `AUTH_GOOGLE_005` |
| Google payload hợp lệ, email tồn tại (FACEBOOK) | Throw `AUTH_GOOGLE_004` |
| Invalid token | Throw `AUTH_GOOGLE_002` |
| Email not verified | Throw `AUTH_GOOGLE_003` |
| Locked account | Throw `AUTH_005` |
| Inactive account | Throw `AUTH_006` |
| Username collision | Sinh username mới không trùng |
| **Set password** khi `hasLocalPassword=false` | Set password thành công, đổi thành `true` |
| **Set password** khi `hasLocalPassword=true` | Throw `AUTH_025` |
| **Change password** khi `hasLocalPassword=true` | Đổi password, cần `currentPassword` đúng |
| **Change password** khi `hasLocalPassword=false` | Throw `AUTH_026` |

### 15.2 Integration Test

- Mock `GoogleTokenVerifier` để trả payload hợp lệ.
- Gọi `POST /api/v1/auth/google`.
- Assert response có `accessToken`, `refreshToken`, `account.email`.
- Assert DB có account, role assignment, customer profile.
- Test case: email mới, email trùng local, email trùng Google cùng sub, email trùng Google khác sub.

### 15.3 Manual Test

1. Cấu hình `GOOGLE_CLIENT_ID`.
2. Chạy backend.
3. Chạy frontend có Google button.
4. Click Continue with Google.
5. Kiểm tra response login.
6. Gọi `/api/v1/auth/me` bằng access token.
7. Refresh token bằng `/api/v1/auth/refresh-token`.

---

## 16. Rủi Ro Và Giảm Thiểu

| Risk | Impact | Mitigation |
|------|--------|------------|
| Client ID sai | Login thất bại | Validate startup nếu env rỗng ở prod |
| Email trùng account local | User có thể login bằng Google | Chỉ cho phép nếu `email_verified=true`; log audit |
| Password placeholder | User không thể login password nếu chưa set | Thêm flow set password sau này |
| Google service outage | Google login unavailable | Login password vẫn hoạt động |
| Token bị log | Lộ thông tin nhạy cảm | Không log raw ID token |
| Race condition username | Tạo account thất bại | Unique constraint DB + retry |
| Provider conflict (FACEBOOK) | Link sai account | CanLink check + AUTH_GOOGLE_004 |

---

## 17. Kế Hoạch Rollout

1. Thêm dependency, config, DTO, verifier, service, controller.
2. Permit endpoint `/api/v1/auth/google`.
3. Viết unit/integration test.
4. Cấu hình Google Cloud client ID cho dev.
5. Test frontend local.
6. Deploy staging với env `GOOGLE_CLIENT_ID`.
7. QA login mới và login account cũ.
8. Deploy production.

---

## 18. Acceptance Criteria

- `POST /api/v1/auth/google` nhận Google ID token hợp lệ và trả `LoginResponse`.
- Account mới từ Google có status `ACTIVE`, role `CUSTOMER`, scope `SYSTEM`.
- Account đã tồn tại theo email có thể login bằng Google nếu status hợp lệ.
- Account `LOCKED`/`INACTIVE` bị chặn.
- Google ID token invalid/expired/audience sai bị từ chối.
- Email dùng bởi provider khác (FACEBOOK) bị từ chối với 409.
- Google sub không khớp account hiện có bị từ chối.
- Response token đúng format với login password hiện tại.
- Không lưu/log raw Google ID token.

---

## 19. Future Enhancements

- Thêm `auth_provider` và `provider_id` vào `ia_account`. (Đã làm trong v1.0)
- Thêm account linking/unlinking trong profile.
- Thêm Facebook/Apple login với provider abstraction.
- Cho user set password sau khi tạo account bằng Google.
- Thêm audit log chi tiết cho social login.
- Xử lý race condition mạnh hơn (retry pattern cho username).
