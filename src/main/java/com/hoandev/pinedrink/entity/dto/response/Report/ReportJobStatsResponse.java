package com.hoandev.pinedrink.entity.dto.response.Report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportJobStatsResponse {
    private long total;
    private long completed;
    private long running;
    private long failed;
}
