package com.hoandev.pinedrink.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filters incoming requests to extract and validate JWT tokens
 * from the Authorization header and sets the security context.
 */
@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider,
                         CustomUserDetailsService customUserDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
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
                        // Regular access token: has username claim
                        String username = claims.get("username", String.class);
                        log.debug("Access token validated for username: {}", username);
                        userPrincipal = (UserPrincipal) customUserDetailsService.loadUserByUsername(username);
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
