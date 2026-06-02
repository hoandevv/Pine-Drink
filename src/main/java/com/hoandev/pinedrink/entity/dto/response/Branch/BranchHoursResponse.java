package com.hoandev.pinedrink.entity.dto.response.Branch;

import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchHoursResponse {
    private String id;
    private String branchId;
    private int dayOfWeek;
    private LocalTime openTime;
    private LocalTime closeTime;
    private boolean closed;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
