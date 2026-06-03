# Technical Design Document - Google OAuth Login

| **Field** | **Value** |
|-----------|-----------|
| **Document ID** | TDD-PINE-AUTH-004 |
| **Project** | Pine Drink - Hệ thống order đồ uống online |
| **Module** | Authentication |
| **Version** | 1.0 |
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
| Account bị khóa | 403 | `AUTH_005` | Account `LOCKED` |
| Account chưa active | 403 | `AUTH_006` | Account `INACTIVE` |
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

```sql
ALTER TABLE ia_account
    ADD COLUMN auth_provider VARCHAR(30) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN provider_id VARCHAR(150) NULL,
    ADD UNIQUE KEY uk_ia_account_provider (auth_provider, provider_id);
```

| Column | Local Account | Google Account |
|--------|---------------|----------------|
| `auth_provider` | `LOCAL` | `GOOGLE` |
| `provider_id` | null | Google `sub` |
| `password` | BCrypt password | Random encoded placeholder hoặc nullable nếu đổi schema |

Khuyến nghị giai đoạn đầu: dùng phương án tối thiểu để giảm rủi ro. Sau đó thêm `auth_provider/provider_id` nếu cần account linking.

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

```text
googleLogin(idToken)
  payload = googleTokenVerifier.verify(idToken)
  nếu payload null -> throw invalid token
  nếu email_verified != true -> throw email not verified

  email = lowercase(payload.email)
  account = accountRepository.findByEmail(email)
      hoặc tạo Google account mới

  validateAccountStatus(account)
  permissionCacheService.invalidateUserCache(account.id)
  principal = customUserDetailsService.buildPrincipal(account)
  accessToken = jwtTokenProvider.generateAccessToken(principal)
  refreshToken = jwtTokenProvider.generateRefreshToken()
  saveRefreshToken(account, refreshToken)
  account.lastLoginAt = now
  save account
  return authMapper.toLoginResponse(accessToken, refreshToken, account)
```

### 10.8 GoogleTokenVerifier Component

Component đề xuất: `GoogleTokenVerifier`.

Nhiệm vụ:

- Đọc `app.oauth2.google.client-id`.
- Verify token bằng `GoogleIdTokenVerifier`.
- Trả payload đã validate.
- Không log raw token.

Pseudo-code:

```java
GoogleIdToken idToken = verifier.verify(rawIdToken);
if (idToken == null) {
    throw new BaseException(ErrorCode.AUTH_GOOGLE_002);
}
Payload payload = idToken.getPayload();
if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
    throw new BaseException(ErrorCode.AUTH_GOOGLE_003);
}
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

### 11.2 Gán Role

Gán role mặc định:

- Role code: `CUSTOMER`.
- Scope: `SYSTEM` với `branch_id=null`.
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

---

## 12. Hướng Dẫn Frontend

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

---

## 13. Cấu Hình Google Cloud

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

## 14. Chiến Lược Test

### 14.1 Unit Test

| Test | Expected |
|------|----------|
| Google payload hợp lệ, email mới | Tạo account, role, profile, trả token |
| Google payload hợp lệ, email tồn tại | Login account hiện có |
| Invalid token | Throw `AUTH_GOOGLE_002` |
| Email not verified | Throw `AUTH_GOOGLE_003` |
| Locked account | Throw `AUTH_005` |
| Username collision | Sinh username mới không trùng |

### 14.2 Integration Test

- Mock `GoogleTokenVerifier` để trả payload hợp lệ.
- Gọi `POST /api/v1/auth/google`.
- Assert response có `accessToken`, `refreshToken`, `account.email`.
- Assert DB có account, role assignment, customer profile.

### 14.3 Manual Test

1. Cấu hình `GOOGLE_CLIENT_ID`.
2. Chạy backend.
3. Chạy frontend có Google button.
4. Click Continue with Google.
5. Kiểm tra response login.
6. Gọi `/api/v1/auth/me` bằng access token.
7. Refresh token bằng `/api/v1/auth/refresh-token`.

---

## 15. Rủi Ro Và Giảm Thiểu

| Risk | Impact | Mitigation |
|------|--------|------------|
| Client ID sai | Login thất bại | Validate startup nếu env rỗng ở prod |
| Email trùng account local | User có thể login bằng Google | Chỉ cho phép nếu `email_verified=true`; log audit |
| Password placeholder | User không thể login password nếu chưa set | Thêm flow set password sau này |
| Google service outage | Google login unavailable | Login password vẫn hoạt động |
| Token bị log | Lộ thông tin nhạy cảm | Không log raw ID token |

---

## 16. Kế Hoạch Rollout

1. Thêm dependency, config, DTO, verifier, service, controller.
2. Permit endpoint `/api/v1/auth/google`.
3. Viết unit/integration test.
4. Cấu hình Google Cloud client ID cho dev.
5. Test frontend local.
6. Deploy staging với env `GOOGLE_CLIENT_ID`.
7. QA login mới và login account cũ.
8. Deploy production.

---

## 17. Acceptance Criteria

- `POST /api/v1/auth/google` nhận Google ID token hợp lệ và trả `LoginResponse`.
- Account mới từ Google có status `ACTIVE`, role `CUSTOMER`, scope `SYSTEM`.
- Account đã tồn tại theo email có thể login bằng Google nếu status hợp lệ.
- Account `LOCKED`/`INACTIVE` bị chặn.
- Google ID token invalid/expired/audience sai bị từ chối.
- Response token đúng format với login password hiện tại.
- Không lưu/log raw Google ID token.

---

## 18. Future Enhancements

- Thêm `auth_provider` và `provider_id` vào `ia_account`.
- Thêm account linking/unlinking trong profile.
- Thêm Facebook/Apple login với provider abstraction.
- Cho user set password sau khi tạo account bằng Google.
- Thêm audit log chi tiết cho social login.
