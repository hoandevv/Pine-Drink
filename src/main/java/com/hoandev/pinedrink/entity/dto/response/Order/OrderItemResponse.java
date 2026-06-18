package com.hoandev.pinedrink.entity.dto.response.Order;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private String id;
    
    private String productId;
    private String productCode;
    private String productName;
    
    private String variantId;
    private String variantName;
    
    private Integer quantity;
    private String sugarLevel;
    private String iceLevel;
    private String note;
    
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    
    @Builder.Default
    private List<OrderItemToppingResponse> toppings = new ArrayList<>();
}
