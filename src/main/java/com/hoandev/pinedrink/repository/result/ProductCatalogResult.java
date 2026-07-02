package com.hoandev.pinedrink.repository.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
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
}
