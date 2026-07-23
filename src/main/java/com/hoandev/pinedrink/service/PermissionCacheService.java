package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.repository.RolePermissionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Cache-aside service for permission authorities.
 * JWT keeps identity and roles, while permissions are loaded from Redis with DB fallback.
 */
@Service
@Slf4j
public class PermissionCacheService {

    private static final String CACHE_KEY_PREFIX = "auth:permissions:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    private static final String CACHE_VALUE_DELIMITER = "\n";

    private final StringRedisTemplate redisTemplate;
    private final RolePermissionRepository rolePermissionRepository;

    public PermissionCacheService(StringRedisTemplate redisTemplate,
                                   RolePermissionRepository rolePermissionRepository) {
        this.redisTemplate = redisTemplate;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    public List<String> getPermissionAuthorities(String accountId) {
        String cacheKey = CACHE_KEY_PREFIX + accountId;

        try {
            String cachedAuthorities = redisTemplate.opsForValue().get(cacheKey);
            if (cachedAuthorities != null) {
                log.debug("Permission cache hit for accountId={}", accountId);
                if (cachedAuthorities.isBlank()) {
                    return List.of();
                }
                return Arrays.stream(cachedAuthorities.split(CACHE_VALUE_DELIMITER))
                        .filter(authority -> !authority.isBlank())
                        .toList();
            }
        } catch (Exception e) {
            try {
                redisTemplate.delete(cacheKey);
            } catch (Exception deleteException) {
                log.warn("Permission cache delete failed for corrupted key accountId={}", accountId, deleteException);
            }
            log.warn("Permission cache read failed for accountId={}, falling back to DB", accountId, e);
        }

        log.debug("Permission cache miss for accountId={}, loading from DB", accountId);
        // Load permissions from DB
        List<String> authorities = rolePermissionRepository.findActivePermissionCodesByAccountId(accountId, LocalDateTime.now())
                .stream()
                .map(permissionCode -> "PERM_" + permissionCode)
                .distinct()
                .toList();

        try {
            // cache permissions redis
            redisTemplate.opsForValue().set(cacheKey, String.join(CACHE_VALUE_DELIMITER, authorities), CACHE_TTL);
        } catch (Exception e) {
            log.warn("Permission cache write failed for accountId={}", accountId, e);
        }
        return authorities;
    }

    public void invalidateUserCache(String accountId) {
        redisTemplate.delete(CACHE_KEY_PREFIX + accountId);
        log.info("Invalidated permission cache for accountId={}", accountId);
    }

    public void invalidateUserCaches(List<String> accountIds) {
        List<String> cacheKeys = accountIds.stream()
                .map(id -> CACHE_KEY_PREFIX + id)
                .toList();
        redisTemplate.delete(cacheKeys);
        log.info("Invalidated permission cache for {} accounts", accountIds.size());
    }
}
