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
public class ProductVariantRequest {
    @NotBlank
    private String variantCode;

    @NotBlank
    private String variantName;

    @NotBlank
    private String sizeLabel;

    @NotNull
    private BigDecimal priceDelta;

    private int displayOrder;
}
