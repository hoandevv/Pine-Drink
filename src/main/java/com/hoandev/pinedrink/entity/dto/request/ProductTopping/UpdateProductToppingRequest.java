package com.hoandev.pinedrink.entity.dto.request.ProductTopping;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProductToppingRequest {

    private Boolean isDefault;

    @Min(value = 1, message = "Max quantity must be greater than or equal to 1")
    private Integer maxQuantity;
}
