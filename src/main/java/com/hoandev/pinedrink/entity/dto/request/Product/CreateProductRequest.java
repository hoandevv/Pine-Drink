package com.hoandev.pinedrink.entity.dto.request.Product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProductRequest {

    @Size(max = 50, message = "Product code must be at most 50 characters")
    private String code;

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name must be at most 255 characters")
    private String name;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    @Size(max = 1000, message = "Image URL must be at most 1000 characters")
    private String imageUrl;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Base price must be greater than or equal to 0")
    private BigDecimal basePrice;

    @Min(value = 0, message = "Preparation minutes must be greater than or equal to 0")
    private Integer preparationMinutes = 10;

    private boolean featured;

    private boolean bestSeller;

    @Size(max = 50, message = "Available ice levels must be at most 50 characters")
    private String availableIceLevels = "0,30,50,70,100";

    @Size(max = 50, message = "Available sugar levels must be at most 50 characters")
    private String availableSugarLevels = "0,30,50,70,100";


    @NotBlank(message = "Category ID is required")
    private String categoryId;
}
