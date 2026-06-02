package com.hoandev.pinedrink.entity.dto.request.ProductVariant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class UpdateProductVariantRequest {

    @Size(max = 100, message = "Variant name must be at most 100 characters")
    private String variantName;

    @Size(max = 30, message = "Size label must be at most 30 characters")
    private String sizeLabel;

    @DecimalMin(value = "0.0", inclusive = true, message = "Price delta must be greater than or equal to 0")
    private BigDecimal priceDelta;

    @Min(value = 0, message = "Display order must be greater than or equal to 0")
    private Integer displayOrder;
}
