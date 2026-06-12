package com.hoandev.pinedrink.entity.dto.response.DailyStock;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DailyStockResponse {
    private String id;
    private String branchId;
    private String branchName;
    private String productId;
    private String productName;
    private String variantId;
    private String variantName;
    private LocalDate stockDate;
    private int dailyQuantity;
    private int soldQuantity;
    private int reservedQuantity;
    private int availableQuantity;
    private String stockStatus;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
