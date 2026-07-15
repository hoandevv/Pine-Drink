package com.hoandev.pinedrink.entity.dto.response.Report;

import com.hoandev.pinedrink.entity.dto.response.Category.CategoryOptionResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReportOptionsResponse {
    private List<CategoryOptionResponse> categories;
    private ReportJobStatsResponse stats;
}
