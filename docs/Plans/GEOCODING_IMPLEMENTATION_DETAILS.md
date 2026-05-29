# Chi Tiết Triển Khai Geocoding Service

## 1. Class Diagram

```
┌─────────────────────────────────────┐
│   <<interface>>                     │
│   GeocodingService                  │
├─────────────────────────────────────┤
│ + geocode(address): Result          │
│ + reverseGeocode(lat,lng): Result   │
│ + isAvailable(): boolean            │
│ + getProviderName(): String         │
└──────────────┬──────────────────────┘
               │
               │ implements
               │
    ┌──────────┴──────────┬──────────────────┐
    │                     │                  │
┌───▼────────────┐  ┌────▼──────────┐  ┌───▼──────────┐
│ Nominatim      │  │ GoogleMaps    │  │ Mapbox       │
│ Service        │  │ Service       │  │ Service      │
└────────────────┘  └───────────────┘  └──────────────┘
```

## 2. Code Examples

### 2.1. GeocodingService Interface

```java
package com.hoandev.pinedrink.service.geocoding;

import java.util.Optional;

public interface GeocodingService {
    
    /**
     * Chuyển địa chỉ thành tọa độ
     */
    Optional<GeocodingResult> geocode(String address);
    
    /**
     * Chuyển tọa độ thành địa chỉ
     */
    Optional<GeocodingResult> reverseGeocode(double latitude, double longitude);
    
    /**
     * Kiểm tra provider có khả dụng không
     */
    boolean isAvailable();
    
    /**
     * Tên provider
     */
    String getProviderName();
}
```

### 2.2. GeocodingResult DTO

```java
package com.hoandev.pinedrink.entity.dto.geocoding;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class GeocodingResult {
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String formattedAddress;
    private String city;
    private String district;
    private String ward;
    private String country;
    private String provider;
    private Double confidence;
}
```

### 2.3. NominatimGeocodingService Implementation

```java
package com.hoandev.pinedrink.service.geocoding.impl;

import com.hoandev.pinedrink.service.GeocodingService;
import com.hoandev.pinedrink.entity.dto.geocoding.GeocodingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.Optional;

@Service("nominatimGeocodingService")
@RequiredArgsConstructor
@Slf4j
public class NominatimGeocodingService implements GeocodingService {

    private final RestTemplate restTemplate;

    @Value("${geocoding.nominatim.base-url:https://nominatim.openstreetmap.org}")
    private String baseUrl;

    @Value("${geocoding.nominatim.user-agent:PineDrinkApp/1.0}")
    private String userAgent;

    @Override
    public Optional<GeocodingResult> geocode(String address) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/search")
                    .queryParam("q", address)
                    .queryParam("format", "json")
                    .queryParam("limit", 1)
                    .queryParam("addressdetails", 1)
                    .build()
                    .toUriString();

            // Thêm User-Agent header (bắt buộc cho Nominatim)
            var headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", userAgent);
            var entity = new org.springframework.http.HttpEntity<>(headers);

            var response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    NominatimResponse[].class
            );

            if (response.getBody() != null && response.getBody().length > 0) {
                NominatimResponse result = response.getBody()[0];
                return Optional.of(mapToGeocodingResult(result));
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Nominatim geocoding failed for address: {}", address, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<GeocodingResult> reverseGeocode(double latitude, double longitude) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/reverse")
                    .queryParam("lat", latitude)
                    .queryParam("lon", longitude)
                    .queryParam("format", "json")
                    .queryParam("addressdetails", 1)
                    .build()
                    .toUriString();

            var headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", userAgent);
            var entity = new org.springframework.http.HttpEntity<>(headers);

            var response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    NominatimResponse.class
            );

            if (response.getBody() != null) {
                return Optional.of(mapToGeocodingResult(response.getBody()));
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Nominatim reverse geocoding failed for lat: {}, lng: {}",
                    latitude, longitude, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            restTemplate.getForEntity(baseUrl + "/status", String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "Nominatim";
    }

    private GeocodingResult mapToGeocodingResult(NominatimResponse response) {
        return GeocodingResult.builder()
                .latitude(new BigDecimal(response.getLat()))
                .longitude(new BigDecimal(response.getLon()))
                .formattedAddress(response.getDisplayName())
                .city(response.getAddress() != null ? response.getAddress().getCity() : null)
                .district(response.getAddress() != null ? response.getAddress().getSuburb() : null)
                .ward(response.getAddress() != null ? response.getAddress().getQuarter() : null)
                .country(response.getAddress() != null ? response.getAddress().getCountry() : null)
                .provider("Nominatim")
                .confidence(response.getImportance())
                .build();
    }

    // Inner class cho response
    @Data
    private static class NominatimResponse {
        private String lat;
        private String lon;
        private String displayName;
        private Double importance;
        private AddressDetails address;

        @Data
        private static class AddressDetails {
            private String city;
            private String suburb;
            private String quarter;
            private String country;
        }
    }
}
```

### 2.4. GeocodingCacheService

```java
package com.hoandev.pinedrink.service.geocoding;

import com.hoandev.pinedrink.entity.dto.geocoding.GeocodingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingCacheService {

    private final RedisTemplate<String, GeocodingResult> redisTemplate;
    
    private static final String CACHE_PREFIX = "geocoding:";
    private static final Duration CACHE_TTL = Duration.ofDays(30);

    public Optional<GeocodingResult> get(String address) {
        try {
            String key = CACHE_PREFIX + address.toLowerCase().trim();
            GeocodingResult result = redisTemplate.opsForValue().get(key);
            
            if (result != null) {
                log.debug("Cache hit for address: {}
