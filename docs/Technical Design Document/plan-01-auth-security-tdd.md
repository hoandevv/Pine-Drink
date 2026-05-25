# Technical Design Document — Auth & Security Foundation

| **Field** | **Value** |
|-----------|-----------|
| **Document ID** | TDD-PINE-AUTH-001 |
| **Project** | Pine Drink — Hệ thống order đồ uống online |
| **Module** | Authentication & Authorization |
| **Version** | 1.0 |
| **Status** | Draft |
| **Author** | Pine Drink Dev Team |
| **Last Updated** | 2026-05-24 |

---

## 1. Title

**Hệ thống Authentication & Authorization cho Pine Drink Platform**

---

## 2. Overview

Pine Drink là hệ thống order đồ uống online multi-brand/multi-branch. Tài liệu này mô tả chi tiết giải pháp xác thực (authentication) và phân quyền (authorization) cho toàn bộ hệ thống, bao gồm JWT token-based authentication, role-based access control (RBAC), rate limiting, và xử lý exception tập trung.

Hệ thống hỗ trợ 3 nhóm người dùng chính:
- **Customer**: Khách hàng đặt đồ uống qua web
- **Staff/Nhân viên**: Xử lý đơn hàng tại kitchen screen
- **Admin**: Quản trị hệ thống, brand, chi nhánh

---

## 3. Purpose

- Đảm bảo chỉ người dùng hợp lệ mới truy cập được API
- Phân quyền chính xác theo vai trò (admin, staff, customer)
- Bảo vệ hệ thống khỏi brute-force qua rate limiting
- Xử lý lỗi nhất quán, trả về response chuẩn
- Cho phép mở rộng sau này (OAuth2, SSO, v.v.)

---

## 4. Scope

### Trong phạm vi (In Scope)
- JWT access token (ngắn hạn) + refresh token (dài hạn)
- Đăng ký, đăng nhập, refresh token, đăng xuất, đổi mật khẩu
- RBAC với 3 vai trò: CUSTOMER, STAFF, ADMIN
- Rate limiting dùng Redis
- Global exception handler
- Custom annotation `@CurrentUser` và `@RateLimit`

### Ngoài phạm vi (Out of Scope)
- OAuth2 / Social login (Google, Facebook)
- SMS OTP / 2-factor authentication
- Single Sign-On (SSO)
- Phân quyền chi tiết cấp brand/branch (sẽ làm ở phase sau)

---

## 5. Audience

| Đối tượng | Vai trò |
|-----------|---------|
| Developer Backend | Implement giải pháp |
| Developer Frontend | Tích hợp API auth |
| QA / Tester | Viết test case |
| DevOps | Cấu hình secret, deployment |
| Technical Leader | Review giải pháp |

---

## 6. Background

### 6.1 Vấn đề
Hệ thống Pine Drink cần bảo vệ các API backend khỏi truy cập trái phép. Spring Boot cung cấp Spring Security làm nền tảng, nhưng cần cấu hình JWT thay vì session-based authentication để phù hợp với kiến trúc REST API stateless.

### 6.2 Giải pháp hiện tại
Chưa có. Dự án đang ở giai đoạn khởi tạo, đã có entity, repository, DTO. Cần xây dựng từ đầu.

### 6.3 Ràng buộc
- Spring Boot 4.0.6 + Spring Security 6.x (không dùng WebSecurityConfigurerAdapter cũ)
- Sử dụng JJWT library (io.jsonwebtoken)
- Redis đã có sẵn trong docker-compose
- Entity Account + Role + Permission + Scope đã có

---

## 7. Requirements

### 7.1 Functional Requirements

| # | Yêu cầu | Mô tả |
|---|---------|-------|
| FR1 | Đăng ký | Customer tự đăng ký tài khoản với username, password, email |
| FR2 | Đăng nhập | Xác thực username/password, trả về JWT access token + refresh token |
| FR3 | Refresh token | Dùng refresh token để lấy access token mới khi hết hạn |
| FR4 | Đăng xuất | Revoke refresh token, xóa khỏi DB |
| FR5 | Đổi mật khẩu | Xác thực mật khẩu cũ, cập nhật mật khẩu mới |
| FR6 | Phân quyền | Endpoint chỉ cho phép user có role phù hợp |
| FR7 | Rate limiting | Giới hạn số request trong khoảng thời gian |

### 7.2 Non-Functional Requirements

| # | Yêu cầu | Mô tả |
|---|---------|-------|
| NFR1 | Bảo mật | Mật khẩu mã hóa BCrypt, JWT ký HMAC-SHA512 |
| NFR2 | Hiệu năng | JWT verify dưới 5ms, rate limit check dưới 2ms |
| NFR3 | Stateless | Access token tự chứa thông tin, không cần query DB mỗi request |
| NFR4 | Audit | Ghi log mọi lần đăng nhập thất bại |

### 7.3 Security Requirements

| # | Yêu cầu | Giá trị |
|---|---------|---------|
| SR1 | Access token TTL | 15 phút |
| SR2 | Refresh token TTL | 7 ngày |
| SR3 | Mã hóa password | BCrypt (strength = 10) |
| SR4 | Rate limit login | 5 lần/phút/IP |
| SR5 | Rate limit register | 3 lần/giờ/IP |
| SR6 | Secret key | Base64-encoded, ≥ 256 bits |

---

## 8. Design / Giải pháp thiết kế

### 8.1 Kiến trúc tổng quan

```
┌──────────────┐     ┌──────────────────────────────────────────────────────┐
│   Client     │     │                   Backend                            │
│  (React App) │     │                                                      │
│              │     │  ┌──────────┐   ┌────────────┐   ┌───────────────┐   │
│  POST /login │────►│  │  Auth    │   │ JWT Auth   │   │ Account +     │   │
│  {username,  │     │  │Controller│──►│  Filter    │──►│ Role Service  │   │
│   password}  │     │  └──────────┘   └────────────┘   └───────────────┘   │
│              │     │                                                      │
│  {JWT}       │◄────│  ┌────────────────┐   ┌──────────────────────────┐   │
│              │     │  │GlobalException │   │     RateLimitAspect      │   │
│              │     │  │   Handler      │   │  (Redis atomic + TTL)    │   │
│              │     │  └────────────────┘   └──────────────────────────┘   │
└──────────────┘     └──────────────────────────────────────────────────────┘
```

### 8.2 JWT Token Design

#### Access Token
```
Header:  { "alg": "HS512", "typ": "JWT" }
Payload: {
    "sub": "account_id (UUID)",
    "username": "nguyenvana",
    "email": "a@example.com",
    "role": "CUSTOMER",
    "iat": 1716512345,
    "exp": 1716513245   // +15 phút
}
Signature: HMAC-SHA512(base64(header) + "." + base64(payload), secret)
```

#### Refresh Token
- Random 64 bytes → Base64 URL-safe (không phải JWT)
- SHA-256 hash lưu trong DB (ia_refresh_token.token_hash)
- Khi refresh: tìm bằng hash → verify chưa revoked, chưa expired → cấp cặp mới → revoke cũ

### 8.3 Luồng xác thực

```
1. REGISTER
   Client                     Backend
     │                          │
     │── POST /auth/register ──►│
     │   {username, password,   │ Validate: username + email unique
     │    fullName, email,      │ Mã hóa password BCrypt
     │    phone}                │ Tạo Account (status=ACTIVE)
     │                          │ Gán role CUSTOMER mặc định
     │◄── 201 + AccountResponse─│
     │                          │

2. LOGIN
     │── POST /auth/login ─────►│
     │   {username, password}   │ Validate credentials
     │                          │ Tạo access token (15 phút)
     │                          │ Tạo refresh token (7 ngày)
     │                          │ Lưu refresh token hash vào DB
     │◄── 200 + LoginResponse ──│
     │   {accessToken,          │
     │    refreshToken,         │
     │    expiresIn,            │
     │    account}              │

3. AUTHENTICATED REQUEST
     │── GET /api/v1/orders ───►│
     │   Authorization: Bearer  │ JwtAuthenticationFilter:
     │   <accessToken>          │  - Extract token từ header
     │                          │  - Validate signature + expiry
     │                          │  - Parse claims → UserPrincipal
     │                          │  - Set SecurityContext
     │                          │  → Cho phép request đi tiếp
     │◄── 200 + data ───────────│

4. REFRESH TOKEN
     │── POST /auth/refresh ───►│
     │   {refreshToken}         │ Hash refresh token
     │                          │ Tìm trong ia_refresh_token
     │                          │ Verify: chưa revoked, chưa expired
     │                          │ Revoke token cũ
     │                          │ Tạo cặp access + refresh mới
     │◄── 200 + LoginResponse ──│

5. LOGOUT
     │── POST /auth/logout ────►│
     │   {refreshToken}         │ Revoke refresh token
     │◄── 200 ok ───────────────│
```

### 8.4 Role-Based Access Control (RBAC)

```
CustomUserDetailsService:
  loadUserById(id) → Account từ DB
                    → Role từ AccountRoleAssignment
                    → UserPrincipal(account, authorities)

@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasRole('STAFF')")
@PreAuthorize("hasRole('CUSTOMER')")
@PreAuthorize("isAuthenticated()")
@PreAuthorize("permitAll()")
```

### 8.5 Package Structure

```
com.hoandev.pinedrink
├── configuration/
│   ├── SecurityConfig.java
│   ├── RedisConfig.java
├── security/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   ├── CustomUserDetailsService.java
│   ├── UserPrincipal.java
│   ├── CurrentUser.java
│   ├── CurrentUserResolver.java
│   └── ratelimit/
│       ├── RateLimit.java
│       ├── RateLimitAspect.java
│       └── BucketConfig.java
├── service/
│   ├── AuthService.java
│   └── impl/
│       └── AuthServiceImpl.java
├── controller/
│   └── AuthController.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── BadRequestException.java
│   ├── UnauthorizedException.java
│   ├── DuplicateResourceException.java
│   ├── BusinessException.java
│   ├── TooManyRequestsException.java
│   └── ErrorResponse.java
└── utils/
    └── Constants.java
```

---

## 9. Details / Chi tiết triển khai

### 9.1 JwtTokenProvider

**File**: `security/JwtTokenProvider.java`

| Method | Input | Output | Mô tả |
|--------|-------|--------|-------|
| `generateAccessToken` | UserPrincipal | String (JWT) | Tạo JWT với subject = accountId, claims = role, email |
| `generateRefreshToken` | void | String | Random 64 bytes, Base64 URL-safe |
| `getAccountIdFromToken` | String token | String | Parse JWT, trả về subject |
| `validateToken` | String token | boolean | Verify signature + expiry |

**Key generation**: Dùng `io.jsonwebtoken.security.Keys.hmacShaKeyFor()` từ secret key Base64.

### 9.2 JwtAuthenticationFilter

**File**: `security/JwtAuthenticationFilter.java`

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        // 1. Lấy Authorization header
        // 2. Extract Bearer token
        // 3. Validate token signature + expiry
        // 4. Parse accountId từ claims
        // 5. Load UserDetails từ DB (hoặc cache)
        // 6. Tạo UsernamePasswordAuthenticationToken
        // 7. Set SecurityContextHolder
        // 8. filterChain.doFilter()
    }
}
```

**Cache optimization**: Có thể cache UserPrincipal trong Redis để tránh query DB mỗi request. Key: `user:{accountId}`, TTL: 15 phút (bằng access token).

### 9.3 SecurityConfig

**File**: `configuration/SecurityConfig.java`

**Public endpoints** (không cần auth):
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/public/**` (menu, brand, branch info)
- `GET /api/v1/brands/**` (công khai)
- `GET /api/v1/branches/**` (công khai)
- `GET /api/v1/products/**` (công khai)
- `GET /api/v1/categories/**` (công khai)
- `/ws/**` (WebSocket)
- `/swagger-ui/**`, `/v3/api-docs/**` (Swagger)

**Protected endpoints**:
- `POST /api/v1/staff/**` → STAFF, ADMIN
- `POST/PUT/DELETE /api/v1/admin/**` → ADMIN
- Mọi endpoint khác → authenticated

### 9.4 UserPrincipal

**File**: `security/UserPrincipal.java`

```java
public class UserPrincipal implements UserDetails {
    private String id;
    private String username;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private boolean enabled;

    // Từ Account + Role:
    // - isEnabled() = "ACTIVE".equals(account.getStatus())
    // - getAuthorities() = role list từ AccountRoleAssignment
}
```

### 9.5 @CurrentUser Annotation

**File**: `security/CurrentUser.java` + `security/CurrentUserResolver.java`

Cho phép inject UserPrincipal trực tiếp vào controller method:

```java
@GetMapping("/me")
public ApiResponse<AccountResponse> getCurrentUser(@CurrentUser UserPrincipal user) {
    return ApiResponse.ok(accountService.getById(user.getId()));
}
```

### 9.6 AuthService

**File**: `service/AuthService.java` + `service/impl/AuthServiceImpl.java`

| Method | Input | Output | Logic |
|--------|-------|--------|-------|
| `register` | RegisterRequest | AccountResponse | Validate unique → BCrypt hash → Save → Gán role CUSTOMER |
| `login` | LoginRequest | LoginResponse | Find account → Verify password → Generate tokens → Save refresh token hash |
| `refresh` | String refreshToken | LoginResponse | Hash token → Find in DB → Check revoked/expired → Revoke cũ → Generate mới |
| `logout` | String refreshToken | void | Hash token → Revoke (set revoked_at) |
| `changePassword` | String accountId, String oldPwd, String newPwd | void | Verify old password → Hash new → Update |

### 9.7 Register Flow

```
1. Validate: username unique, email unique, phone unique
2. Mã hóa password: BCryptPasswordEncoder.encode(password)
3. Tạo Account: id=UUID, status=ACTIVE
4. Tìm role CUSTOMER (hoặc tạo nếu chưa có)
5. Tạo AccountRoleAssignment: account + role + scope (SYSTEM)
6. Trả về AccountResponse
```

### 9.8 Rate Limiting (Bucket4j + Redis)

**Files**:
- `security/ratelimit/RateLimit.java` — annotation
- `security/ratelimit/RateLimitAspect.java` — aspect xử lý
- `security/ratelimit/BucketConfig.java` — cấu hình bucket4j + Redisson

#### Giải pháp
Dùng **Bucket4j** (token bucket algorithm) + **Redisson** (distributed Redis client) để rate limiting đồng bộ giữa nhiều instances. Không dùng fixed window đơn giản vì:
- Token bucket cho phép burst request trong ngắn hạn
- Distributed: lock qua Redis, tất cả instances đều check chung 1 bucket
- Linh hoạt: mỗi endpoint có bandwidth + capacity riêng

#### Bucket4j Configuration

```java
@Configuration
public class BucketConfig {

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://redis:6379")
            .setPassword(System.getenv("REDIS_PASSWORD"));
        return Redisson.create(config);
    }

    @Bean
    public ProxyManager<String> proxyManager(RedissonClient redisson) {
        return new RedissonProxyManager<>(redisson);
    }
}
```

#### RateLimit Annotation

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    String key();
    long capacity() default 10;     // Số token tối đa trong bucket
    long refillTokens() default 5;  // Số token thêm vào mỗi lần refill
    long refillSeconds() default 60; // Chu kỳ refill
}
```

#### RateLimitAspect

```java
@Aspect
@Component
public class RateLimitAspect {

    private final Map<String, Bucket> localBuckets = new ConcurrentHashMap<>();
    @Autowired private ProxyManager<String> proxyManager;

    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String key = rateLimit.key() + ":" + getClientIp();

        // Distributed bucket qua Redis
        Bucket bucket = proxyManager.builder().build(key, createBucketConfig(rateLimit));

        if (bucket.tryConsume(1)) {
            return pjp.proceed();
        }

        throw new TooManyRequestsException("Rate limit exceeded. Vui lòng thử lại sau.");
    }

    private BucketConfiguration createBucketConfig(RateLimit rateLimit) {
        Refill refill = Refill.intervally(rateLimit.refillTokens(), Duration.ofSeconds(rateLimit.refillSeconds()));
        Bandwidth limit = Bandwidth.classic(rateLimit.capacity(), refill);
        return BucketConfiguration.builder().addLimit(limit).build();
    }

    private String getClientIp() {
        // Lấy IP từ RequestContextHolder
    }
}
```

#### Endpoint Rate Limit Config

| Endpoint | Key | Capacity | Refill | Chu kỳ |
|----------|-----|----------|--------|--------|
| POST `/auth/login` | `login:{ip}` | 5 | 5 | 60s |
| POST `/auth/register` | `register:{ip}` | 3 | 3 | 3600s |
| POST `/auth/refresh` | `refresh:{ip}` | 10 | 10 | 60s |
| POST `/orders` | `order:{customerId}` | 20 | 20 | 60s |
| Default | `default:{ip}` | 100 | 100 | 60s |

```java
@RateLimit(key = "login", capacity = 5, refillTokens = 5, refillSeconds = 60)
@PostMapping("/login")
public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) { ... }

@RateLimit(key = "register", capacity = 3, refillTokens = 3, refillSeconds = 3600)
@PostMapping("/register")
public ApiResponse<AccountResponse> register(@Valid @RequestBody RegisterRequest request) { ... }
```

### 9.9 Global Exception Handler

**File**: `exception/GlobalExceptionHandler.java`

| Exception | HTTP Status | Response |
|-----------|-------------|----------|
| `ResourceNotFoundException` | 404 | `{ success: false, message: "…" }` |
| `BadRequestException` | 400 | `{ success: false, message: "…", errors: […] }` |
| `UnauthorizedException` | 401 | `{ success: false, message: "Unauthorized" }` |
| `DuplicateResourceException` | 409 | `{ success: false, message: "…" }` |
| `TooManyRequestsException` | 429 | `{ success: false, message: "Rate limit exceeded" }` |
| `BusinessException` | 422 | `{ success: false, message: "…" }` |
| `MethodArgumentNotValidException` | 400 | `{ success: false, message: "Validation failed", errors: [field errors] }` |
| `AccessDeniedException` | 403 | `{ success: false, message: "Forbidden" }` |
| `AuthenticationException` | 401 | `{ success: false, message: "Invalid credentials" }` |
| `Exception` (fallback) | 500 | `{ success: false, message: "Internal server error" }` |

### 9.10 Application Configuration

```yaml
# application.yaml
jwt:
  secret: ${JWT_SECRET:base64-encoded-secret-key-at-least-256-bits}
  access-token-expiration: 900000    # 15 phút
  refresh-token-expiration: 604800000 # 7 ngày

bucket4j:
  filters:
    - cache-name: rate-limit-buckets
      enabled: true
      strategy: first-non-waited

spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}

rate-limit:
  login:
    capacity: 5
    refill-tokens: 5
    refill-seconds: 60
  register:
    capacity: 3
    refill-tokens: 3
    refill-seconds: 3600
```

---

## 10. Risks / Rủi ro

| # | Rủi ro | Tác động | Khả năng | Giải pháp |
|---|--------|----------|----------|-----------|
| R1 | JWT secret bị lộ | Toàn bộ hệ thống bị compromised | Thấp | Dùng environment variable, không hardcode. Rotate secret định kỳ |
| R2 | Refresh token bị đánh cắp | Attacker có thể refresh token vô hạn | Trung bình | Revoke token cũ mỗi khi refresh. Giới hạn TTL 7 ngày. Xoay vòng token |
| R3 | Redis down | Rate limiting không hoạt động | Thấp | Bucket4j tự động fallback sang local bucket (in-memory ConcurrentHashMap) với capacity nhỏ hơn. Nếu cả 2 đều fail → cho phép request đi tiếp (fail-open) |
| R4 | Brute force login | Tài khoản bị đoán password | Cao | Rate limiting + khóa tài khoản sau N lần sai (future phase) |
| R5 | JWT không được revoke ngay | User bị logout nhưng token vẫn dùng được đến khi hết hạn | Thấp | Access token TTL ngắn (15 phút). Blacklist cho token bị revoke (optional) |
| R6 | Token trong URL log | Access token lộ trong access log | Trung bình | Không bao giờ truyền token qua query param. Sanitize log |

---

## 11. Testing / Kiểm thử

### 11.1 Unit Tests (JUnit 5 + Mockito)

| Test case | Mô tả |
|-----------|-------|
| `JwtTokenProviderTest.generateAndValidateToken` | Generate token → validate → parse → đúng accountId |
| `JwtTokenProviderTest.expiredToken` | Token hết hạn → validate false |
| `JwtTokenProviderTest.invalidSignature` | Token sai secret → validate false |
| `AuthServiceTest.registerSuccess` | Đăng ký hợp lệ → account + role CUSTOMER |
| `AuthServiceTest.registerDuplicateUsername` | Username đã tồn tại → DuplicateResourceException |
| `AuthServiceTest.loginSuccess` | Login đúng → access + refresh token |
| `AuthServiceTest.loginWrongPassword` | Sai mật khẩu → BadRequestException |
| `AuthServiceTest.refreshTokenSuccess` | Refresh hợp lệ → cặp token mới, token cũ bị revoke |
| `AuthServiceTest.refreshTokenExpired` | Refresh token hết hạn → UnauthorizedException |
| `AuthServiceTest.refreshTokenRevoked` | Refresh token đã revoked → UnauthorizedException |
| `RateLimitAspectTest.exceedLimit` | Vượt quá rate limit → TooManyRequestsException |
| `RateLimitAspectTest.burstThenCoolDown` | Gọi nhanh 10 request → request thứ 11 bị chặn → đợi refill → request tiếp theo được |
| `BucketConfigTest.distributedBucket` | 2 bucket instances cùng key → consume chung 1 bucket (test với Redisson) |

### 11.2 Integration Tests (Testcontainers)

| Test case | Mô tả |
|-----------|-------|
| `AuthControllerTest.registerAndLogin` | Gọi register → login → nhận token → gọi /me thành công |
| `AuthControllerTest.refreshFlow` | Login → refresh → token cũ revoked → access mới hoạt động |
| `AuthControllerTest.protectedEndpoint` | Không gửi token → 401 |
| `RateLimitIntegrationTest` | Gọi login > 5 lần → bị rate limit |

### 11.3 Test Data

```sql
-- Account test
INSERT INTO ia_account (id, username, password, full_name, email, phone, status)
VALUES ('test-id-1', 'testuser',
        '$2a$10$...', 'Test User',
        'test@pine.com', '0900000000', 'ACTIVE');

-- Role test
INSERT INTO ia_role (id, code, name, role_type)
VALUES ('role-cust-1', 'CUSTOMER', 'Customer', 'SYSTEM');

-- Assignment
INSERT INTO ia_account_role_assignment (id, account_id, role_id, scope_id, status)
VALUES ('assign-1', 'test-id-1', 'role-cust-1', 'scope-sys-1', 'ACTIVE');
```

---

## 12. Deployment / Triển khai

### 12.1 Prerequisites

- Docker Compose (MySQL + Redis)
- Environment variables:
  ```
  JWT_SECRET=<base64-encoded-secret>
  ```
- Redis container đang chạy (cho rate limiting)

### 12.2 Configuration

```yaml
# application.yml (production)
jwt:
  secret: ${JWT_SECRET}
  access-token-expiration: 900000
  refresh-token-expiration: 604800000

spring:
  datasource:
    url: jdbc:mysql://mysql:3306/pine_drink?useSSL=false
    username: ${MYSQL_USER}
    password: ${MYSQL_PASSWORD}
  data:
    redis:
      host: redis
      port: 6379
      password: ${REDIS_PASSWORD}
```

### 12.3 Build & Run

```bash
# Build
./mvnw clean package -DskipTests

# Run với Docker
docker-compose up -d mysql redis
java -jar target/pine-drink-0.0.1-SNAPSHOT.jar

# Hoặc chạy full stack
docker-compose up -d
```

### 12.4 Verify

```bash
# Health check
curl http://localhost:8080/actuator/health

# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456","fullName":"Test","email":"test@test.com"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'
```

---

## 13. Monitoring / Theo dõi

### 13.1 Metrics (Spring Actuator)

| Metric | Mô tả |
|--------|-------|
| `counter.auth.login.success` | Số lần login thành công |
| `counter.auth.login.failed` | Số lần login thất bại |
| `counter.auth.token.refresh` | Số lần refresh token |
| `counter.ratelimit.blocked` | Số request bị rate limit chặn |
| `gauge.jwt.validation.time` | Thời gian validate token (ms) |

### 13.2 Logging

```yaml
logging:
  level:
    com.hoandev.pinedrink.security: DEBUG
    com.hoandev.pinedrink.exception: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

**Audit log**: Ghi `ia_audit_log` cho:
- Login thành công
- Login thất bại (sai password)
- Refresh token
- Change password

### 13.3 Alerts

| Điều kiện | Hành động |
|-----------|-----------|
| Login failed rate > 10/phút/IP | Cảnh báo brute force |
| Rate limit block > 100 request/phút | Cảnh báo DDoS |
| JWT validation error rate > 5% | Kiểm tra JWT secret |

---

## 14. FAQ / Câu hỏi thường gặp

### Q1: Tại sao không dùng session-based authentication?

Spring Security session-based authentication không phù hợp với kiến trúc REST API stateless. JWT cho phép:
- Scale ngang (horizontal scaling) không cần共享 session
- Mobile app không hỗ trợ cookie tốt
- Microservice-friendly

### Q2: Tại sao refresh token không phải là JWT?

Refresh token là opaque string (random bytes) vì:
- Có thể revoke được (cần check DB)
- Không cần parse claims
- Bảo mật hơn (không chứa thông tin trong token)

### Q3: Làm sao để revoke access token ngay lập tức?

Có 2 cách:
1. **Blacklist**: Dùng Redis set lưu JTI (JWT ID) của token bị revoke. Check mỗi request.
2. **Short TTL**: Access token chỉ sống 15 phút, đợi hết hạn tự nhiên.

Plan hiện tại dùng cách 2 (đơn giản hơn). Có thể nâng cấp lên blacklist sau.

### Q4: Xử lý rate limit khi Redis down?

Bucket4j có 2 cấp độ:
1. **Local bucket** (in-memory `ConcurrentHashMap`): fallback khi Redis unavailable
2. **Distributed bucket** (Redisson + Redis): đồng bộ giữa các instances

Cấu hình `RateLimitAspect`:
- Redis OK → dùng distributed bucket
- Redis down → tự động fallback sang local bucket (với capacity nhỏ hơn để an toàn)
- Cả Redis + local đều fail → **FAIL_OPEN**: cho phép request đi tiếp

### Q5: bucket4j khác gì so với Redis atomic increment thông thường?

| Tiêu chí | Redis INCR (fixed window) | Bucket4j (token bucket) |
|-----------|--------------------------|------------------------|
| Thuật toán | Fixed window: reset mỗi N giây | Token bucket: refill liên tục |
| Burst | Không cho phép burst | Cho phép burst trong capacity |
| Distributed | Cần tự implement | Redisson proxy built-in |
| Fallback | Không có | Local bucket fallback |
| Linh hoạt | 1 window cố định | Nhiều bandwidth + refill phức tạp |

### Q6: Làm sao để test rate limiting?

```bash
# Gọi login 6 lần trong 60s
for i in $(seq 1 6); do
  curl -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"wrong"}'
  echo ""
done
# Lần thứ 6 → 429 Too Many Requests

# Test burst: capacity=5, gọi 10 request liên tiếp
for i in $(seq 1 10); do
  curl -s -o /dev/null -w "%{http_code}\n" \
    -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"wrong"}'
done
# 5 request đầu: 401 (sai pass) — 5 request sau: 429 (rate limit)
```

---

## 15. Appendix / Phụ lục

### 15.1 Libraries

```xml
<!-- JJWT (JWT) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>

<!-- Spring Boot Starter (đã có) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Rate Limiting (Bucket4j + Redisson) -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.10.1</version>
</dependency>
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-jcache</artifactId>
    <version>8.10.1</version>
</dependency>
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.34.0</version>
</dependency>
```

### 15.2 Entity Relationships Reference

```
ia_account (1) ──► ia_account_role_assignment (n) ──► ia_role (n)
    │
    └──► ia_refresh_token (n)
```

### 15.3 JWT Secret Generation

```bash
# Linux/Mac
openssl rand -base64 64

# Windows (PowerShell)
[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(64))
```

### 15.4 API Error Response Format

```json
// Validation error
{
    "success": false,
    "message": "Validation failed",
    "errors": [
        { "field": "email", "message": "Email không hợp lệ" },
        { "field": "password", "message": "Mật khẩu phải có ít nhất 6 ký tự" }
    ]
}

// Business error
{
    "success": false,
    "message": "Tên đăng nhập đã tồn tại"
}

// Rate limit
{
    "success": false,
    "message": "Too many requests. Vui lòng thử lại sau 30 giây",
    "retryAfter": 30
}

// Server error
{
    "success": false,
    "message": "Internal server error",
    "traceId": "abc-123-def"
}
```

### 15.5 References

- [Spring Security Architecture](https://docs.spring.io/spring-security/reference/servlet/architecture.html)
- [JJWT GitHub](https://github.com/jwtk/jjwt)
- [JSON Web Token RFC 7519](https://datatracker.ietf.org/doc/html/rfc7519)
- [Redis Rate Limiting Patterns](https://redis.io/glossary/rate-limiting/)
- [Bucket4j Documentation](https://bucket4j.com/)
- [Redisson Documentation](https://redisson.org/)
