package com.hoandev.pinedrink.entity.dto.response.DailyStock;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PublicDailyStockResponse {
    private String branchId;
    private String productId;
    private String productName;
    private String variantId;
    private String variantName;
    private LocalDate stockDate;
    private int availableQuantity;
    private String stockStatus;
    private String status;
}
