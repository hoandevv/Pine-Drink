package com.hoandev.pinedrink.entity.dto.response.Product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private String id;
    private String code;
    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal basePrice;
    private int preparationMinutes;
    private boolean featured;
    private boolean bestSeller;
    private String availableIceLevels;
    private String availableSugarLevels;
    private String brandId;
    private String brandName;
    private String categoryId;
    private String categoryName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
