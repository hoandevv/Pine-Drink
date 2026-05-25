package com.hoandev.pinedrink.entity.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {
    private String productId;

    @NotBlank
    private String productCode;

    @NotBlank
    private String productName;

    private String variantName;

    @Min(1)
    private int quantity;

    private String sugarLevel;
    private String iceLevel;
    private String note;

    @NotNull
    private BigDecimal unitPrice;

    private List<OrderItemToppingRequest> toppings;
}
