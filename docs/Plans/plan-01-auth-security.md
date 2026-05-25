# Plan 1 — Auth & Security Foundation

## Mục tiêu
Xây dựng nền tảng authentication và authorization cho toàn bộ hệ thống Pine Drink. Bao gồm JWT access/refresh token, role-based access control, rate limiting, exception handling, và các tiện ích bảo mật.

## Files cần tạo/sửa

### Cấu hình Security
- `configuration/SecurityConfig.java` — Spring Security config với JWT filter, CORS, CSRF disable
- `security/JwtTokenProvider.java` — generate token, validate, parse claims
- `security/JwtAuthenticationFilter.java` — extends OncePerRequestFilter
- `security/CustomUserDetailsService.java` — load user từ DB
- `security/UserPrincipal.java` — implements UserDetails
- `security/CurrentUser.java` — @interface + resolver

### Exception Handling
- `exception/GlobalExceptionHandler.java` — @ControllerAdvice xử lý toàn bộ exception
- `exception/ResourceNotFoundException.java`
- `exception/BadRequestException.java`
- `exception/UnauthorizedException.java`
- `exception/DuplicateResourceException.java`
- `exception/BusinessException.java`
- `exception/ErrorResponse.java`

### Service & Controller
- `service/AuthService.java` + `service/impl/AuthServiceImpl.java`
- `controller/AuthController.java` — POST /login, /register, /refresh, /logout, /change-password

### DTO (review)
- `entity/dto/request/LoginRequest.java`
- `entity/dto/response/LoginResponse.java`

### Rate Limiting
- `security/ratelimit/RateLimit.java` — annotation
- `security/ratelimit/RateLimitAspect.java` — aspect dùng Redis atomic increment + TTL
- `configuration/RedisConfig.java` — Redis template config

### Utils
- `utils/Constants.java` — hằng số role, status, pattern

## Chi tiết Implementation

### JWT Flow

```
Login → validate credentials → generate access token (15 phút) + refresh token (7 ngày)
Refresh → validate refresh token hash trong DB → revoke cũ → generate mới
Logout → revoke refresh token (xóa khỏi DB)
```

#### JwtTokenProvider.java

```java
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    public String generateAccessToken(UserPrincipal userPrincipal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .setSubject(userPrincipal.getId().toString())
                .claim("role", userPrincipal.getAuthorities())
                .claim("email", userPrincipal.getEmail())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshToken() {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Long.parseLong(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

#### JwtAuthenticationFilter.java

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String token = getTokenFromRequest(request);

        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
            Long userId = tokenProvider.getUserIdFromToken(token);
            UserDetails userDetails = customUserDetailsService.loadUserById(userId);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

#### SecurityConfig.java

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Autowired private CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"Unauthorized\"}");
                })
            )
            .addFilterBefore(jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
```

### Rate Limiting với Redis

Dùng Redis atomic increment + TTL để implement rate limiting:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    String key() default "";
    int maxRequests() default 100;
    int windowSeconds() default 60;
}

@Aspect
@Component
public class RateLimitAspect {

    @Autowired private StringRedisTemplate redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit)
            throws Throwable {
        String key = rateLimit.key();
        if (key.isEmpty()) {
            key = joinPoint.getSignature().toShortString();
        }

        String redisKey = "ratelimit:" + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);

        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, rateLimit.windowSeconds(), TimeUnit.SECONDS);
        }

        if (count != null && count > rateLimit.maxRequests()) {
            throw new TooManyRequestsException("Rate limit exceeded");
        }

        return joinPoint.proceed();
    }
}
```

### Constants.java

```java
public final class Constants {

    private Constants() {}

    // Roles
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_STAFF = "STAFF";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    // Entity status
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_DELETED = "DELETED";

    // Order status
    public static final String ORDER_PENDING = "PENDING";
    public static final String ORDER_CONFIRMED = "CONFIRMED";
    public static final String ORDER_PREPARING = "PREPARING";
    public static final String ORDER_READY = "READY";
    public static final String ORDER_COMPLETED = "COMPLETED";
    public static final String ORDER_CANCELLED = "CANCELLED";

    // Payment status
    public static final String PAYMENT_PENDING = "PENDING";
    public static final String PAYMENT_PAID = "PAID";
    public static final String PAYMENT_FAILED = "FAILED";
    public static final String PAYMENT_REFUNDED = "REFUNDED";

    // Regex patterns
    public static final String PHONE_PATTERN = "^(0[3|5|7|8|9])[0-9]{8}$";
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

    // JWT
    public static final long ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000;   // 15 phút
    public static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000; // 7 ngày

    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;
}
```

## API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | /api/v1/auth/register | Đăng ký tài khoản |
| POST | /api/v1/auth/login | Đăng nhập, trả về access + refresh token |
| POST | /api/v1/auth/refresh | Refresh access token |
| POST | /api/v1/auth/logout | Revoke refresh token |
| POST | /api/v1/auth/change-password | Đổi mật khẩu (cần old password) |

## Checklist

- [ ] Tạo SecurityConfig với JWT filter chain, CORS, CSRF disable
- [ ] Tạo JwtTokenProvider: generate, validate, parse
- [ ] Tạo JwtAuthenticationFilter extends OncePerRequestFilter
- [ ] Tạo CustomUserDetailsService load user từ DB
- [ ] Tạo UserPrincipal implements UserDetails
- [ ] Tạo @CurrentUser annotation + CurrentUserResolver
- [ ] Tạo GlobalExceptionHandler + các exception class
- [ ] Tạo Constants utils với role, status, pattern
- [ ] Tạo @RateLimit annotation + aspect với Redis
- [ ] Tạo RedisConfig
- [ ] Tạo AuthService + AuthController
- [ ] Review LoginRequest / LoginResponse DTO
- [ ] Cấu hình application.yml: jwt secret, expiration time
- [ ] Test register → login → refresh → logout flow
- [ ] Test rate limiting hoạt động
- [ ] Test GlobalExceptionHandler trả về đúng format
