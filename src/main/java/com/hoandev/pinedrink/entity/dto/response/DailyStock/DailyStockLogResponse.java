package com.hoandev.pinedrink.entity.dto.response.DailyStock;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DailyStockLogResponse {
    private String id;
    private String dailyStockId;
    private String orderId;
    private String actionType;
    private int quantity;
    private int beforeDailyQuantity;
    private int afterDailyQuantity;
    private int beforeSoldQuantity;
    private int afterSoldQuantity;
    private int beforeReservedQuantity;
    private int afterReservedQuantity;
    private String reason;
    private String createdBy;
    private LocalDateTime createdAt;
}
