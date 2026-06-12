package com.hoandev.pinedrink.entity.dto.request.DailyStock;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SetDailyStockQuotaRequest {

    @NotBlank(message = "Branch id is required")
    private String branchId;

    @NotBlank(message = "Variant id is required")
    private String variantId;

    @NotNull(message = "Stock date is required")
    @FutureOrPresent(message = "Stock date must be today or in the future")
    private LocalDate stockDate;

    @NotNull(message = "Daily quantity is required")
    @Min(value = 0, message = "Daily quantity must be greater than or equal to 0")
    private Integer dailyQuantity;

    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String reason;
}