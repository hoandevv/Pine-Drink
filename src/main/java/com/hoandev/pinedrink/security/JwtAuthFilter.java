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
/** Request gửi vào
 ↓
 Lấy JWT từ header
 ↓
 Kiểm tra JWT
 ↓
 Đọc thông tin trong JWT
 ↓
 Load tài khoản
 ↓
 Tạo Authentication
 ↓
 Lưu vào SecurityContext
 ↓
 Cho request đi tiếp
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

    private UserPrincipal createUserPrincipal(Claims claims) {
        String tokenType = claims.get("type", String.class);

        if (RESET_TOKEN_TYPE.equals(tokenType)) {
            return loadPrincipalFromResetToken(claims);
        }

        return loadPrincipalFromAccessToken(claims);
    }

    private UserPrincipal loadPrincipalFromResetToken(Claims claims) {
        String userId = claims.getSubject();

        log.debug("Reset token validated for userId: {}", userId);

        return (UserPrincipal) customUserDetailsService.loadUserById(userId);
    }

    private UserPrincipal loadPrincipalFromAccessToken(Claims claims) {
        String accountId = claims.getSubject();
        List<String> roles = extractRoles(claims);

        log.debug("Access token validated for accountId: {}", accountId);

        return customUserDetailsService.buildPrincipal(
                customUserDetailsService.loadAccountById(accountId),
                roles
        );
    }

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

    private String extractToken(HttpServletRequest request) {
        String authorizationHeader =
                request.getHeader(AUTHORIZATION_HEADER);

        if (!StringUtils.hasText(authorizationHeader)
                || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authorizationHeader.substring(BEARER_PREFIX.length());
    }

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