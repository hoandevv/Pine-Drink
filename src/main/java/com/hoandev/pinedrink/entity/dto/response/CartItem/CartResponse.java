package com.hoandev.pinedrink.entity.dto.response.CartItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private String id;
    private String status;
    private String branchId;
    private String branchName;
    private String customerId;
    @Builder.Default
    private List<CartItemResponse> items = new ArrayList<>();
    private Integer totalQuantity;
    private BigDecimal subtotalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
