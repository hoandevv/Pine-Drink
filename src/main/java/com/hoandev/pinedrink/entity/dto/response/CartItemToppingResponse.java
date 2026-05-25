package com.hoandev.pinedrink.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemToppingResponse {
    private String id;
    private String toppingId;
    private String toppingName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
