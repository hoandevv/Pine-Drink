package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.repository.RolePermissionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
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

    private final RedisTemplate<String, Object> redisTemplate;
    private final RolePermissionRepository rolePermissionRepository;

    public PermissionCacheService(RedisTemplate<String, Object> redisTemplate,
                                  RolePermissionRepository rolePermissionRepository) {
        this.redisTemplate = redisTemplate;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    public List<String> getPermissionAuthorities(String accountId) {
        String cacheKey = CACHE_KEY_PREFIX + accountId;

        @SuppressWarnings("unchecked")
        List<String> cachedAuthorities = (List<String>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedAuthorities != null) {
            log.debug("Permission cache hit for accountId={}", accountId);
            return cachedAuthorities;
        }

        log.debug("Permission cache miss for accountId={}, loading from DB", accountId);
        List<String> authorities = rolePermissionRepository.findActivePermissionCodesByAccountId(accountId, LocalDateTime.now())
                .stream()
                .map(permissionCode -> "PERM_" + permissionCode)
                .distinct()
                .toList();

        redisTemplate.opsForValue().set(cacheKey, authorities, CACHE_TTL);
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
