package com.hoandev.pinedrink.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {
    @NotBlank
    private String brandId;

    @NotBlank
    private String categoryId;

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    private String description;
    private String imageUrl;

    @NotNull
    private BigDecimal basePrice;

    private int preparationMinutes;
    private boolean isFeatured;
    private boolean isBestSeller;
}
