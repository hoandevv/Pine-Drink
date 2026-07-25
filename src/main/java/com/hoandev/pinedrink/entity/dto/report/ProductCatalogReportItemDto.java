package com.hoandev.pinedrink.entity.dto.report;

import lombok.Getter;

@Getter
public class ProductCatalogReportItemDto {
    private String productCode;
    private String productName;
    private String categoryName;
    private String basePrice;
    private String status;
    private String preparationMinutes;
    private String featured;
    private String bestSeller;
    private String variants;
    private String createdAt;

    public ProductCatalogReportItemDto(String productCode, String productName, String categoryName, String basePrice, String status, String preparationMinutes, String featured, String bestSeller, String variants, String createdAt) {
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
