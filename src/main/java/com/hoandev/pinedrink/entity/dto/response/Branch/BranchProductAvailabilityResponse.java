package com.hoandev.pinedrink.entity.dto.response.Branch;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchProductAvailabilityResponse {
    private String id;
    private String branchId;
    private String productId;
    private String productName;
    private boolean available;
    private BigDecimal salePrice;
    private String soldOutReason;
    private LocalDateTime availableFrom;
    private LocalDateTime availableTo;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
