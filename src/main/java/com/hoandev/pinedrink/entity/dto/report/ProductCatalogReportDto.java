package com.hoandev.pinedrink.entity.dto.report;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProductCatalogReportDto {
    private String title;
    private String generatedAt;
    private String statusFilter;
    private String categoryFilter;
    private String totalProducts;
    private List<ProductCatalogReportItemDto> items;
}
