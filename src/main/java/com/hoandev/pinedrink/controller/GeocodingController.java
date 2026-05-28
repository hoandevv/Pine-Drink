package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.geocoding.GeocodingResult;
import com.hoandev.pinedrink.entity.dto.request.Address.SearchAddressRequest;
import com.hoandev.pinedrink.entity.dto.response.Address.AddressSearchResult;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.service.GeocodingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for geocoding operations.
 * Provides endpoints for address search and reverse geocoding.
 */
@RestController
@RequestMapping("/api/v1/geocoding")
@RequiredArgsConstructor
@Slf4j
public class GeocodingController {

    @Qualifier("nominatimGeocodingService")
    private final GeocodingService geocodingService;

    /**
     * Search for addresses matching a query.
     * Used for autocomplete/search functionality in map picker.
     *
     * @param request the search request
     * @return list of matching addresses
     */
    @PostMapping("/search")
    public ResponseEntity<BaseResponse<List<AddressSearchResult>>> searchAddress(
            @Valid @RequestBody SearchAddressRequest request) {
        
        log.info("Searching addresses for query: {}", request.getQuery());
        
        int limit = request.getLimit() != null ? request.getLimit() : 5;
        List<GeocodingResult> results = geocodingService.searchAddress(request.getQuery(), limit);
        
        List<AddressSearchResult> searchResults = results.stream()
                .map(this::mapToSearchResult)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(BaseResponse.success(
                searchResults, 
                "Found " + searchResults.size() + " addresses"
        ));
    }

    /**
     * Reverse geocode coordinates to address.
     * Used when user drags pin on map.
     *
     * @param latitude the latitude
     * @param longitude the longitude
     * @return the address at the coordinates
     */
    @GetMapping("/reverse")
    public ResponseEntity<BaseResponse<AddressSearchResult>> reverseGeocode(
            @RequestParam 
            @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
            @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
            double latitude,
            
            @RequestParam 
            @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
            @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
            double longitude) {
        
        log.info("Reverse geocoding for lat: {}, lng: {}", latitude, longitude);
        
        Optional<GeocodingResult> result = geocodingService.reverseGeocode(latitude, longitude);
        
        if (result.isEmpty()) {
            return ResponseEntity.ok(BaseResponse.success(
                    null, 
                    "No address found at the specified coordinates"
            ));
        }
        
        AddressSearchResult searchResult = mapToSearchResult(result.get());
        
        return ResponseEntity.ok(BaseResponse.success(
                searchResult, 
                "Address found successfully"
        ));
    }

    /**
     * Check if geocoding service is available.
     *
     * @return service status
     */
    @GetMapping("/status")
    public ResponseEntity<BaseResponse<Boolean>> checkStatus() {
        boolean available = geocodingService.isAvailable();
        
        return ResponseEntity.ok(BaseResponse.success(
                available, 
                available ? "Geocoding service is available" : "Geocoding service is unavailable"
        ));
    }

    /**
     * Maps GeocodingResult to AddressSearchResult.
     *
     * @param result the geocoding result
     * @return the address search result
     */
    private AddressSearchResult mapToSearchResult(GeocodingResult result) {
        return AddressSearchResult.builder()
                .displayName(result.getFormattedAddress())
                .addressLine(result.getFormattedAddress())
                .ward(result.getWard())
                .district(result.getDistrict())
                .city(result.getCity())
                .country(result.getCountry())
                .latitude(result.getLatitude())
                .longitude(result.getLongitude())
                .confidence(result.getConfidence())
                .provider(result.getProvider())
                .build();
    }
}
