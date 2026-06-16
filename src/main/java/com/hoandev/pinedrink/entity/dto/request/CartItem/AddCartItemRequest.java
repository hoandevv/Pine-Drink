package com.hoandev.pinedrink.entity.dto.request.CartItem;

import jakarta.validation.Valid;
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

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AddCartItemRequest {

    @NotBlank(message = "Branch is required")
    private String branchId;

    @NotBlank(message = "Product is required")
    private String productId;

    private String variantId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private String sugarLevel;

    private String iceLevel;

    @Size(max = 255, message = "Note must not exceed 255 characters")
    private String note;

    @Valid
    @Builder.Default
    private List<CartItemToppingRequest> toppings = new ArrayList<>();
}
