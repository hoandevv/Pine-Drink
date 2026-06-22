package com.hoandev.pinedrink.entity.dto.response.Order;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String id;
    private String orderCode;
    private String status;
    
    // Branch info
    private String branchId;
    private String branchName;
    private String branchAddress;
    
    // Customer info
    private String customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    
    // Order details
    private String orderType;
    private String paymentMethod;
    private String paymentStatus;
    
    // Pricing
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal deliveryFee;
    private BigDecimal totalAmount;
    
    // Timing
    private LocalDateTime pickupTime;
    
    // Delivery
    private String deliveryAddress;
    
    private String note;
    
    // Order items
    @Builder.Default
    private List<OrderItemResponse> items = new ArrayList<>();
    
    // Status timestamps
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime preparedAt;
    private LocalDateTime readyAt;
    private LocalDateTime deliveringAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime rejectedAt;
    
    private String cancelReason;
}
