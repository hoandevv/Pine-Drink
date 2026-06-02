package com.hoandev.pinedrink.entity.dto.response.Branch;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(pattern = "HH:mm")
    private LocalTime openTime;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime closeTime;
    private boolean closed;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
