package com.hoandev.pinedrink.entity.dto.response.CartItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private String id;
    private String productId;
    private String productCode;
    private String productName;
    private String productImageUrl;
    private String variantId;
    private String variantName;
    private Integer quantity;
    private String sugarLevel;
    private String iceLevel;
    private String note;
    private BigDecimal unitPrice;
    private BigDecimal toppingAmount;
    private BigDecimal totalPrice;
    @Builder.Default
    private List<CartItemToppingResponse> toppings = new ArrayList<>();
}
