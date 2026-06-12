package com.hoandev.pinedrink.entity.dto.response.DailyStock;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CopyDailyStockQuotaResponse {
    private String branchId;
    private LocalDate sourceDate;
    private LocalDate targetDate;
    private boolean overwrite;
    private int createdCount;
    private int updatedCount;
    private int skippedCount;
}
