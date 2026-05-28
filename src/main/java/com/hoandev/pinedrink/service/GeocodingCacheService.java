package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.geocoding.GeocodingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Service for caching geocoding results in Redis.
 * Reduces external API calls and improves performance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "geocoding:";
    private static final String SEARCH_PREFIX = "geocoding:search:";
    private static final String REVERSE_PREFIX = "geocoding:reverse:";
    private static final Duration CACHE_TTL = Duration.ofDays(30);

    /**
     * Gets a cached geocoding result for an address.
     *
     * @param address the address to look up
     * @return the cached result if found
     */
    public Optional<GeocodingResult> get(String address) {
        try {
            String key = buildKey(address);
            Object cached = redisTemplate.opsForValue().get(key);

            if (cached instanceof GeocodingResult result) {
                log.debug("Cache hit for address: {}", address);
                return Optional.of(result);
            }

            log.debug("Cache miss for address: {}", address);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Error getting from cache for address: {}", address, e);
            return Optional.empty();
        }
    }

    /**
     * Caches a geocoding result for an address.
     *
     * @param address the address
     * @param result the geocoding result
     */
    public void put(String address, GeocodingResult result) {
        try {
            String key = buildKey(address);
            redisTemplate.opsForValue().set(key, result, CACHE_TTL);
            log.debug("Cached geocoding result for address: {}", address);

        } catch (Exception e) {
            log.error("Error putting to cache for address: {}", address, e);
        }
    }

    /**
     * Gets cached search results for a query.
     *
     * @param query the search query
     * @param limit the result limit
     * @return the cached search results if found
     */
    public Optional<List<GeocodingResult>> getSearchResults(String query, int limit) {
        try {
            String key = buildSearchKey(query, limit);
            Object cached = redisTemplate.opsForValue().get(key);

            if (cached instanceof List<?> results && !results.isEmpty()) {
                log.debug("Cache hit for search query: {}, limit: {}", query, limit);
                return Optional.of(results.stream()
                        .filter(GeocodingResult.class::isInstance)
                        .map(GeocodingResult.class::cast)
                        .toList());
            }

            log.debug("Cache miss for search query: {}, limit: {}", query, limit);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Error getting search results from cache for query: {}", query, e);
            return Optional.empty();
        }
    }

    /**
     * Caches search results for a query.
     *
     * @param query the search query
     * @param limit the result limit
     * @param results the search results
     */
    public void putSearchResults(String query, int limit, List<GeocodingResult> results) {
        try {
            String key = buildSearchKey(query, limit);
            redisTemplate.opsForValue().set(key, results, CACHE_TTL);
            log.debug("Cached search results for query: {}, limit: {}, count: {}", query, limit, results.size());

        } catch (Exception e) {
            log.error("Error putting search results to cache for query: {}", query, e);
        }
    }

    /**
     * Gets cached reverse geocoding result for coordinates.
     *
     * @param latitude the latitude
     * @param longitude the longitude
     * @return the cached result if found
     */
    public Optional<GeocodingResult> getReverseResult(double latitude, double longitude) {
        try {
            String key = buildReverseKey(latitude, longitude);
            Object cached = redisTemplate.opsForValue().get(key);

            if (cached instanceof GeocodingResult result) {
                log.debug("Cache hit for reverse geocoding: lat={}, lng={}", latitude, longitude);
                return Optional.of(result);
            }

            log.debug("Cache miss for reverse geocoding: lat={}, lng={}", latitude, longitude);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Error getting reverse result from cache for lat={}, lng={}", latitude, longitude, e);
            return Optional.empty();
        }
    }

    /**
     * Caches reverse geocoding result for coordinates.
     *
     * @param latitude the latitude
     * @param longitude the longitude
     * @param result the geocoding result
     */
    public void putReverseResult(double latitude, double longitude, GeocodingResult result) {
        try {
            String key = buildReverseKey(latitude, longitude);
            redisTemplate.opsForValue().set(key, result, CACHE_TTL);
            log.debug("Cached reverse geocoding result for lat={}, lng={}", latitude, longitude);

        } catch (Exception e) {
            log.error("Error putting reverse result to cache for lat={}, lng={}", latitude, longitude, e);
        }
    }

    /**
     * Evicts a cached geocoding result for an address.
     *
     * @param address the address
     */
    public void evict(String address) {
        try {
            String key = buildKey(address);
            redisTemplate.delete(key);
            log.debug("Evicted cache for address: {}", address);

        } catch (Exception e) {
            log.error("Error evicting cache for address: {}", address, e);
        }
    }

    /**
     * Builds a cache key from an address.
     *
     * @param address the address
     * @return the cache key
     */
    private String buildKey(String address) {
        return CACHE_PREFIX + address.toLowerCase().trim();
    }

    /**
     * Builds a cache key for search results.
     *
     * @param query the search query
     * @param limit the result limit
     * @return the cache key
     */
    private String buildSearchKey(String query, int limit) {
        return SEARCH_PREFIX + query.toLowerCase().trim() + ":" + limit;
    }

    /**
     * Builds a cache key for reverse geocoding.
     *
     * @param latitude the latitude
     * @param longitude the longitude
     * @return the cache key
     */
    private String buildReverseKey(double latitude, double longitude) {
        // Round to 6 decimal places (~0.1 meter precision)
        String lat = String.format("%.6f", latitude);
        String lng = String.format("%.6f", longitude);
        return REVERSE_PREFIX + lat + ":" + lng;
    }
}
