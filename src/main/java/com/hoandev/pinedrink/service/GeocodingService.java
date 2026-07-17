package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.geocoding.GeocodingResult;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for geocoding operations.
 * Converts addresses to geographic coordinates and vice versa.
 */
public interface GeocodingService {
    
    /**
     * Converts an address string to geographic coordinates.
     *
     * @param address the address to geocode
     * @return the geocoding result if successful
     */
    Optional<GeocodingResult> geocode(String address);
    
    /**
     * Searches for addresses matching a query string.
     * Returns multiple results for autocomplete/search functionality.
     *
     * @param query the search query
     * @param limit maximum number of results
     * @return list of matching geocoding results
     */
    List<GeocodingResult> searchAddress(String query, int limit);
    
    /**
     * Converts geographic coordinates to an address.
     *
     * @param latitude the latitude
     * @param longitude the longitude
     * @return the geocoding result if successful
     */
    Optional<GeocodingResult> reverseGeocode(double latitude, double longitude);
    
    /**
     * Checks if the geocoding provider is available.
     *
     * @return true if available, false otherwise
     */
    boolean isAvailable();
    
    /**
     * Gets the name of the geocoding provider.
     *
     * @return the provider name
     */
    String getProviderName();
}
