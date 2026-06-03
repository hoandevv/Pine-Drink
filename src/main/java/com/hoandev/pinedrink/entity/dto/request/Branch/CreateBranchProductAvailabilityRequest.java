package com.hoandev.pinedrink.entity.dto.request.Branch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBranchProductAvailabilityRequest {

    @NotBlank(message = "Product id is required")
    private String productId;

    @Builder.Default
    private boolean available = true;

    private BigDecimal salePrice;

    @Size(max = 255, message = "Sold out reason must be at most 255 characters")
    private String soldOutReason;

    private LocalDateTime availableFrom;

    private LocalDateTime availableTo;
}
