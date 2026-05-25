package com.hoandev.pinedrink.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private String id;
    private String brandId;
    private String categoryId;
    private String categoryName;
    private String code;
    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal basePrice;
    private int preparationMinutes;
    private boolean isFeatured;
    private boolean isBestSeller;
    private String status;
    private List<ProductVariantResponse> variants;
}
