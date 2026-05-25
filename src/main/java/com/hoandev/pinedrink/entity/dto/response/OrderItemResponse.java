package com.hoandev.pinedrink.entity.dto.response;

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
public class OrderItemResponse {
    private String id;
    private String productId;
    private String productCode;
    private String productName;
    private String variantName;
    private int quantity;
    private String sugarLevel;
    private String iceLevel;
    private String note;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private List<OrderItemToppingResponse> toppings;
}
