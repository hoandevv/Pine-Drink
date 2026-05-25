package com.hoandev.pinedrink.entity.dto.request;

import jakarta.validation.constraints.Min;
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
public class OrderItemToppingRequest {
    @NotBlank
    private String toppingCode;

    @NotBlank
    private String toppingName;

    @Min(1)
    private int quantity;

    @NotNull
    private BigDecimal unitPrice;
}
