package com.hoandev.pinedrink.entity.dto.response.Report;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReportJobResponse {
    private String id;
    private String reportType;
    private String fileFormat;
    private String status;
    private String fileUrl;
    private String errorMessage;
    private LocalDateTime requestedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
