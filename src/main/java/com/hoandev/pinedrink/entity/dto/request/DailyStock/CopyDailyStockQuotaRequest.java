package com.hoandev.pinedrink.entity.dto.request.DailyStock;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CopyDailyStockQuotaRequest {

    @NotBlank(message = "Branch id is required")
    private String branchId;

    @NotNull(message = "Source date is required")
    private LocalDate sourceDate;

    @NotNull(message = "Target date is required")
    @FutureOrPresent(message = "Target date must be today or in the future")
    private LocalDate targetDate;

    private boolean overwrite = false;

    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String reason;
}