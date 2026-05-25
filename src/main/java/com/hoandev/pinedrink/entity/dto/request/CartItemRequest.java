package com.hoandev.pinedrink.entity.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRequest {
    @NotBlank
    private String productId;

    private String variantId;

    @Min(1)
    private int quantity;

    private String sugarLevel;
    private String iceLevel;
    private String note;
}
