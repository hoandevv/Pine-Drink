package com.hoandev.pinedrink.configuration;

import com.hoandev.pinedrink.security.CustomAccessDeniedHandler;
import com.hoandev.pinedrink.security.CustomAuthenticationEntryPoint;
import com.hoandev.pinedrink.security.JwtAuthFilter;
import com.hoandev.pinedrink.security.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Cấu hình bảo mật chính cho REST API.
 *
 * <p>Dùng JWT stateless, tắt form login/http basic, cấu hình CORS, xử lý lỗi
 * xác thực, phân quyền endpoint, rồi gắn các filter bảo mật vào filter chain.</p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, RateLimitFilter rateLimitFilter, CustomAuthenticationEntryPoint authenticationEntryPoint, CustomAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods}")
    private String allowedMethods;

    @Value("${app.cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age:3600}")
    private long maxAge;

    /**
     * Cung cấp thuật toán mã hóa mật khẩu bằng BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Tạo SecurityFilterChain cho session stateless, CORS, JWT và phân quyền API.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable) // Không dùng session, nên tắt CSRF
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // cấu hình để fe gọi do khác port
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint) // xử lý lỗi xác thực
                        .accessDeniedHandler(accessDeniedHandler) // đã đăng nhập nhưng không có quyền truy cập
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/register/verify-otp",
                                "/api/v1/auth/register/resend-otp",
                                "/api/v1/auth/google",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/refresh-token",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/forgot-password/verify-otp",
                                "/api/v1/payments/momo/ipn",
                                "/api/v1/payments/momo/return",
                                "/api/v1/vouchers/customer/available",
                                "/api/v1/files/private/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/ws/**",
                                "/favicon.ico",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/branches/*/hours", "GET")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/branches/*/hours/**", "GET")).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/products/**",
                                 "/api/v1/categories/**",
                                 "/api/v1/toppings/**",
                                 "/api/v1/branches/active",
                                 "/api/v1/branches/{branchId}/daily-stocks",
                                 "/api/v1/branches/{branchId}/availability/products/**",
                                 "/api/v1/branches/{branchId}/availability/toppings/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // Đọc JWT và xác thực người dùng trước khi request vào controller., filter mặc định của spring
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Tạo cấu hình CORS từ các giá trị trong application config.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(splitConfig(allowedOrigins));
        config.setAllowedMethods(splitConfig(allowedMethods));
        if ("*".equals(allowedHeaders.trim())) {
            config.addAllowedHeader("*");
        } else {
            config.setAllowedHeaders(splitConfig(allowedHeaders));
        }
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Tách chuỗi cấu hình dạng comma-separated thành danh sách giá trị sạch.
     */
    private List<String> splitConfig(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
