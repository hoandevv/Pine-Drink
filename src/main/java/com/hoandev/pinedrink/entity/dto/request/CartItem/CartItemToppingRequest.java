package com.hoandev.pinedrink.entity.dto.request.CartItem;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CartItemToppingRequest {

    @NotBlank(message = "Topping Id is required")
    private String toppingId;

    @NotNull(message = "Topping quantity is required")
    @Min(value = 1, message = "Topping quantity must be at least 1")
    private Integer quantity;
}
