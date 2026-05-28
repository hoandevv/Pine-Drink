package com.hoandev.pinedrink.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;

/**
 * Component responsible for generating and validating JWT tokens.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;
    private final long resetTokenExpirationSeconds;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration:3600}") long accessTokenExpirationSeconds,
            @Value("${app.jwt.refresh-token-expiration:86400}") long refreshTokenExpirationSeconds,
            @Value("${app.jwt.reset-token-expiration:900}") long resetTokenExpirationSeconds
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
        this.resetTokenExpirationSeconds = resetTokenExpirationSeconds;
    }

    /**
     * Generates an access token for the given user principal.
     *
     * @param principal the authenticated user's principal
     * @return a signed JWT access token
     */
    public String generateAccessToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationSeconds * 1000);
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .subject(principal.getId())
                .claim("username", principal.getUsername())
                .claim("email", principal.getEmail())
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * Generates a refresh token without user-specific claims.
     *
     * @return a signed JWT refresh token
     */
    public String generateRefreshToken() {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpirationSeconds * 1000);

        return Jwts.builder()
                .subject("refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * Generates a reset token for password reset flow.
     * Contains userId and a special claim to identify it as a reset token.
     *
     * @param userId the user ID
     * @return a signed JWT reset token
     */
    public String generateResetToken(String userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + resetTokenExpirationSeconds * 1000);

        return Jwts.builder()
                .subject(userId)
                .claim("type", "reset")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * Parses and verifies a JWT token, returning its claims.
     *
     * @param token the JWT token to parse
     * @return the claims extracted from the token
     * @throws JwtException if the token is invalid or expired
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validates whether the given token is structurally and cryptographically valid.
     *
     * @param token the JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Returns the access token expiration duration in seconds.
     *
     * @return expiration time in seconds
     */
    public long getAccessTokenExpiresInSeconds() {
        return accessTokenExpirationSeconds;
    }

    /**
     * Returns the reset token expiration duration in seconds.
     *
     * @return expiration time in seconds
     */
    public long getResetTokenExpiresInSeconds() {
        return resetTokenExpirationSeconds;
    }
}
