package com.hoandev.pinedrink.entity.dto.response.Order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderSummaryResponse {
    private String id;
    private String orderCode;
    private String status;
    private String branchId;
    private String branchName;
    private String customerId;
    private String customerName;
    private String customerPhone;
    private String orderType;
    private String paymentMethod;
    private String paymentStatus;
    private BigDecimal totalAmount;
    private LocalDateTime pickupTime;
    private LocalDateTime createdAt;
}
