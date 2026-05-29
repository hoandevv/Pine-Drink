package com.hoandev.pinedrink.entity.dto.request.Address;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request DTO for searching/autocomplete addresses.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchAddressRequest {

    @NotBlank(message = "Search query is required")
    @Size(min = 3, max = 255, message = "Search query must be between 3 and 255 characters")
    private String query;

    /**
     * Optional: Limit results to a specific area (latitude).
     */
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private BigDecimal latitude;

    /**
     * Optional: Limit results to a specific area (longitude).
     */
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private BigDecimal longitude;

    /**
     * Optional: Search radius in kilometers.
     */
    private Integer radiusKm;

    /**
     * Optional: Maximum number of results (default 5).
     */
    private Integer limit;
}
