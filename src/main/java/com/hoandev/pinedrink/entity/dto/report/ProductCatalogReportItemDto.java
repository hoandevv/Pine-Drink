package com.hoandev.pinedrink.entity.dto.report;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
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
}
