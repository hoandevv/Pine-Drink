package com.hoandev.pinedrink.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 * Filters incoming requests to extract and validate JWT tokens
 * from the Authorization header and sets the security context.
 */
@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider,
                         CustomUserDetailsService customUserDetailsService,
                         ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);

            if (token != null) {
                log.debug("Token found in request: {}", request.getRequestURI());
                
                if (jwtTokenProvider.validateToken(token)) {
                    Claims claims = jwtTokenProvider.parseToken(token);
                    
                    // Check if this is a reset token (has "type": "reset" claim)
                    String tokenType = claims.get("type", String.class);
                    
                    UserPrincipal userPrincipal;
                    if ("reset".equals(tokenType)) {
                        // Reset token: subject is userId, no username claim
                        String userId = claims.getSubject();
                        log.debug("Reset token validated for userId: {}", userId);
                        userPrincipal = (UserPrincipal) customUserDetailsService.loadUserById(userId);
                    } else {
                        // Regular access token: use accountId from subject and roles from claims.
                        String accountId = claims.getSubject();
                        @SuppressWarnings("unchecked")
                        List<String> roleAuthorities = claims.get("roles", List.class);

                        if (roleAuthorities == null || roleAuthorities.isEmpty()) {
                            throw new IllegalStateException("Access token does not contain roles claim");
                        }

                        log.debug("Access token validated for accountId: {}", accountId);
                        userPrincipal = customUserDetailsService.buildPrincipal(
                                customUserDetailsService.loadAccountById(accountId),
                                roleAuthorities
                        );
                    }

                    if (!userPrincipal.isEnabled()) {
                        log.warn("Rejected token for disabled account: accountId={}, status={}",
                                userPrincipal.getId(), userPrincipal.getStatus());
                        writeAuthError(response, ErrorCode.AUTH_006, HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userPrincipal, null, userPrincipal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authentication set for user: {}", userPrincipal.getUsername());
                } else {
                    log.warn("Invalid token for request: {}", request.getRequestURI());
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    private void writeAuthError(HttpServletResponse response, ErrorCode errorCode, int status) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                BaseResponse.error(errorCode.getCode(), errorCode.getMessage())
        ));
    }

    /**
     * Extracts the Bearer token from the Authorization header.
     *
     * @param request the incoming HTTP request
     * @return the token string, or null if not present
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
