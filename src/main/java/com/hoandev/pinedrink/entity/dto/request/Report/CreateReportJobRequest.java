package com.hoandev.pinedrink.entity.dto.request.Report;

import com.hoandev.pinedrink.entity.enums.ReportFileFormat;
import com.hoandev.pinedrink.entity.enums.ReportType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReportJobRequest {
    @NotNull
    private ReportType reportType;

    @NotNull
    private ReportFileFormat fileFormat;

    private String filters;
    private String branchId;
}
