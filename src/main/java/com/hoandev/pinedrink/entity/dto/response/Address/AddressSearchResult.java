package com.hoandev.pinedrink.entity.dto.response.Address;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for address search results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressSearchResult {
    
    private String displayName;
    
    private String addressLine;
    
    private String ward;
    
    private String district;
    
    private String city;
    
    private String country;
    
    private BigDecimal latitude;
    
    private BigDecimal longitude;
    
    /**
     * Confidence score (0.0 - 1.0).
     */
    private Double confidence;
    
    /**
     * Provider name (e.g., "Nominatim").
     */
    private String provider;
}
