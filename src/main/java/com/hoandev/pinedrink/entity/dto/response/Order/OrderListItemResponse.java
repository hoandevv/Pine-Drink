package com.hoandev.pinedrink.entity.dto.response.Order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderListItemResponse {
    private String id;
    private String orderCode;
    private String status;
    private String customerName;
    private String orderType;
    private String paymentStatus;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private Integer totalItems;
    private String itemsPreview;
}
