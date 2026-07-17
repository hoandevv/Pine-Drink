package com.hoandev.pinedrink.entity.dto.response.Order;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemToppingResponse {
    private String id;
    
    private String toppingId;
    private String toppingCode;
    private String toppingName;
    
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
