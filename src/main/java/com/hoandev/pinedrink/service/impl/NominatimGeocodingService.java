package com.hoandev.pinedrink.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hoandev.pinedrink.entity.dto.geocoding.GeocodingResult;
import com.hoandev.pinedrink.service.GeocodingService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Nominatim (OpenStreetMap) implementation of GeocodingService.
 * Free and open-source geocoding service.
 */
@Service("nominatimGeocodingService")
@Slf4j
public class NominatimGeocodingService implements GeocodingService {

    private final RestTemplate restTemplate;

    public NominatimGeocodingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${geocoding.nominatim.base-url:https://nominatim.openstreetmap.org}")
    private String baseUrl;

    @Value("${geocoding.nominatim.user-agent:PineDrinkApp/1.0}")
    private String userAgent;

    @Override
    public Optional<GeocodingResult> geocode(String address) {
        // Validation: check if address is null or blank
        if (address == null || address.isBlank()) {
            log.warn("Geocoding request with null or blank address");
            return Optional.empty();
        }
        
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/search")
                    .queryParam("q", address.trim())
                    .queryParam("format", "json")
                    .queryParam("limit", 1)
                    .queryParam("addressdetails", 1)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", userAgent);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<NominatimResponse[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    NominatimResponse[].class
            );

            if (response.getBody() != null && response.getBody().length > 0) {
                NominatimResponse result = response.getBody()[0];
                log.info("Nominatim geocoding successful for address: {}", address);
                return Optional.of(mapToGeocodingResult(result));
            }

            log.warn("Nominatim geocoding returned no results for address: {}", address);
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

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", userAgent);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<NominatimResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    NominatimResponse.class
            );

            if (response.getBody() != null) {
                log.info("Nominatim reverse geocoding successful for lat: {}, lng: {}", latitude, longitude);
                return Optional.of(mapToGeocodingResult(response.getBody()));
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Nominatim reverse geocoding failed for lat: {}, lng: {}", latitude, longitude, e);
            return Optional.empty();
        }
    }

    @Override
    public List<GeocodingResult> searchAddress(String query, int limit) {
        // Validation: check if query is null or blank
        if (query == null || query.isBlank()) {
            log.warn("Search request with null or blank query");
            return new ArrayList<>();
        }
        
        // Limit validation: ensure limit is between 1 and 10
        int safeLimit = Math.min(Math.max(limit, 1), 10);
        
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/search")
                    .queryParam("q", query.trim())
                    .queryParam("format", "json")
                    .queryParam("limit", safeLimit)
                    .queryParam("addressdetails", 1)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", userAgent);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<NominatimResponse[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    NominatimResponse[].class
            );

            if (response.getBody() != null && response.getBody().length > 0) {
                log.info("Nominatim search returned {} results for query: {}", response.getBody().length, query);
                return Arrays.stream(response.getBody())
                        .map(this::mapToGeocodingResult)
                        .collect(Collectors.toList());
            }

            log.warn("Nominatim search returned no results for query: {}", query);
            return new ArrayList<>();

        } catch (Exception e) {
            log.error("Nominatim search failed for query: {}", query, e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            restTemplate.getForEntity(baseUrl + "/status", String.class);
            return true;
        } catch (Exception e) {
            log.warn("Nominatim service is not available", e);
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

    /**
     * Response DTO for Nominatim API.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class NominatimResponse {
        private String lat;
        private String lon;

        @JsonProperty("display_name")
        private String displayName;

        private Double importance;
        private AddressDetails address;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class AddressDetails {
            private String city;
            private String town;
            private String village;
            private String municipality;
            private String suburb;
            private String quarter;
            private String neighbourhood;
            private String country;
            
            // Helper method to get city name from various fields
            public String getCity() {
                if (city != null) return city;
                if (town != null) return town;
                if (municipality != null) return municipality;
                if (village != null) return village;
                return null;
            }
            
            // Helper method to get district/suburb
            public String getSuburb() {
                if (suburb != null) return suburb;
                if (neighbourhood != null) return neighbourhood;
                return null;
            }
        }
    }
}
