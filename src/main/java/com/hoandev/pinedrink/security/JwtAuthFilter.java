package com.hoandev.pinedrink.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter xác thực JWT cho mỗi request.
 *
 * <p>Đọc Bearer token từ header Authorization, kiểm tra token, lấy thông tin
 * người dùng, rồi lưu Authentication vào SecurityContext để Spring Security
 * xử lý phân quyền ở các bước sau.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String RESET_TOKEN_TYPE = "reset";

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final ObjectMapper objectMapper;

    /**
     * Xác thực request hiện tại bằng JWT trước khi chuyển tiếp filter chain.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            authenticateRequest(request, response);

            if (response.isCommitted()) {
                return;
            }
        } catch (Exception exception) {
            log.error(
                    "Cannot authenticate request {}: {}",
                    request.getRequestURI(),
                    exception.getMessage(),
                    exception
            );
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Trích xuất token, kiểm tra hợp lệ, tạo principal, rồi set authentication.
     */
    private void authenticateRequest(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        String token = extractToken(request);

        if (token == null) {
            return;
        }

        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("Invalid token for request: {}", request.getRequestURI());
            return;
        }

        Claims claims = jwtTokenProvider.parseToken(token);
        UserPrincipal userPrincipal = createUserPrincipal(claims);

        if (!userPrincipal.isEnabled()) {
            rejectDisabledAccount(userPrincipal, response);
            return;
        }

        setAuthentication(userPrincipal, request);
    }

    /**
     * Tạo UserPrincipal theo loại token: reset token hoặc access token.
     */
    private UserPrincipal createUserPrincipal(Claims claims) {
        String tokenType = claims.get("type", String.class);

        if (RESET_TOKEN_TYPE.equals(tokenType)) {
            return loadPrincipalFromResetToken(claims);
        }

        return loadPrincipalFromAccessToken(claims);
    }

    /**
     * Load người dùng từ subject của reset token.
     */
    private UserPrincipal loadPrincipalFromResetToken(Claims claims) {
        String userId = claims.getSubject();

        log.debug("Reset token validated for userId: {}", userId);

        return (UserPrincipal) customUserDetailsService.loadUserById(userId);
    }

    /**
     * Load tài khoản từ access token, kèm danh sách role trong claims.
     */
    private UserPrincipal loadPrincipalFromAccessToken(Claims claims) {
        String accountId = claims.getSubject();
        List<String> roles = extractRoles(claims);

        log.debug("Access token validated for accountId: {}", accountId);

        return customUserDetailsService.buildPrincipal(
                customUserDetailsService.loadAccountById(accountId),
                roles
        );
    }

    /**
     * Lấy danh sách role từ claims của access token.
     */
    private List<String> extractRoles(Claims claims) {
        Object rolesClaim = claims.get("roles");

        if (!(rolesClaim instanceof List<?> roles) || roles.isEmpty()) {
            throw new IllegalStateException(
                    "Access token does not contain roles claim"
            );
        }

        return roles.stream()
                .map(String::valueOf)
                .toList();
    }

    /**
     * Tạo Authentication và lưu vào SecurityContext của request hiện tại.
     */
    private void setAuthentication(
            UserPrincipal userPrincipal,
            HttpServletRequest request
    ) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userPrincipal,
                        null,
                        userPrincipal.getAuthorities()
                );

        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        log.debug(
                "Authentication set for user: {}",
                userPrincipal.getUsername()
        );
    }

    /**
     * Từ chối request khi tài khoản trong token đã bị vô hiệu hóa.
     */
    private void rejectDisabledAccount(
            UserPrincipal userPrincipal,
            HttpServletResponse response
    ) throws IOException {

        log.warn(
                "Rejected token for disabled account: accountId={}, status={}",
                userPrincipal.getId(),
                userPrincipal.getStatus()
        );

        writeAuthError(
                response,
                ErrorCode.AUTH_006,
                HttpServletResponse.SC_FORBIDDEN
        );
    }

    /**
     * Lấy JWT từ header Authorization theo định dạng Bearer token.
     */
    private String extractToken(HttpServletRequest request) {
        String authorizationHeader =
                request.getHeader(AUTHORIZATION_HEADER);

        if (!StringUtils.hasText(authorizationHeader)
                || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authorizationHeader.substring(BEARER_PREFIX.length());
    }

    /**
     * Ghi response lỗi xác thực dạng JSON.
     */
    private void writeAuthError(
            HttpServletResponse response,
            ErrorCode errorCode,
            int status
    ) throws IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        BaseResponse<?> errorResponse = BaseResponse.error(
                errorCode.getCode(),
                errorCode.getMessage()
        );

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
