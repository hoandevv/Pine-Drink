package com.hoandev.pinedrink.entity.dto.response.Branch;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchToppingAvailabilityResponse {
    private String id;
    private String branchId;
    private String toppingId;
    private String toppingName;
    private boolean available;
    private String soldOutReason;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
