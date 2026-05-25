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
public class CartResponse {
    private String id;
    private String customerId;
    private String branchId;
    private String sessionId;
    private List<CartItemResponse> items;
    private BigDecimal totalAmount;
    private String status;
}
