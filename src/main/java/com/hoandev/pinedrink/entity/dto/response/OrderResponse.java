package com.hoandev.pinedrink.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String id;
    private String orderCode;
    private String brandId;
    private String branchId;
    private String branchName;
    private String customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String orderType;
    private String status;
    private String paymentStatus;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal deliveryFee;
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items;
    private LocalDateTime pickupTime;
    private String deliveryAddress;
    private String note;
    private LocalDateTime createdAt;
    private List<OrderStatusHistoryResponse> statusHistory;
}
