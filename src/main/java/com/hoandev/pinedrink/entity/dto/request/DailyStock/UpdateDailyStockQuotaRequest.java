package com.hoandev.pinedrink.entity.dto.request.DailyStock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDailyStockQuotaRequest {

    @NotNull(message = "Daily quantity is required")
    @Min(value = 0, message = "Daily quantity must be greater than or equal to 0")
    private Integer dailyQuantity;

    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String reason;
}