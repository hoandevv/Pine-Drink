package com.hoandev.pinedrink.entity.dto.request.ProductTopping;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class AssignProductToppingRequest {

    @NotBlank(message = "Topping ID is required")
    private String toppingId;

    private boolean isDefault;

    @Min(value = 1, message = "Max quantity must be greater than or equal to 1")
    private Integer maxQuantity = 3;
}
