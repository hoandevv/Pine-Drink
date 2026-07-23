package com.hoandev.pinedrink.repository.result;

import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * Result class for representing product catalog information.
 */
@Getter
public class ProductCatalogResult {
    private final String productCode;
    private final String productName;
    private final String categoryName;
    private final BigDecimal basePrice;
    private final String status;
    private final Integer preparationMinutes;
    private final Boolean featured; 
    private final Boolean bestSeller;
    private final String variants;
    private final LocalDateTime createdAt;

    public ProductCatalogResult(String productCode, String productName, String categoryName, BigDecimal basePrice, String status, Integer preparationMinutes, Boolean featured, Boolean bestSeller, String variants, LocalDateTime createdAt) {
        this.productCode = productCode;
        this.productName = productName;
        this.categoryName = categoryName;
        this.basePrice = basePrice;
        this.status = status;
        this.preparationMinutes = preparationMinutes;
        this.featured = featured;
        this.bestSeller = bestSeller;
        this.variants = variants;
        this.createdAt = createdAt;
    }

}
