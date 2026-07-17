package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.ExportRequest;
import com.hoandev.pinedrink.entity.dto.response.Report.ReportJobResponse;
import org.springframework.stereotype.Component;

@Component
public class ReportJobMapper {

    /**
     * Map entity export request sang DTO response của API.
     *
     * @param job entity export request
     * @return DTO response được trả về bởi các API report job
     */
    public ReportJobResponse toResponse(ExportRequest job) {
        if (job == null) {
            return null;
        }

        return ReportJobResponse.builder()
                .id(job.getId())
                .reportType(job.getReportType())
                .fileFormat(job.getFileFormat())
                .status(job.getStatus())
                .fileUrl(job.getFileUrl())
                .errorMessage(job.getErrorMessage())
                .requestedAt(job.getCreatedAt())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }
}
