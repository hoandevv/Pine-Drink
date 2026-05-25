package com.hoandev.pinedrink.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistoryResponse {
    private String id;
    private String oldStatus;
    private String newStatus;
    private String reason;
    private String changedBy;
    private LocalDateTime changedAt;
}
