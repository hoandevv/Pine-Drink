package com.hoandev.pinedrink.entity.dto.geocoding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO representing the result of a geocoding operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeocodingResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
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
