package com.hoandev.pinedrink.entity.dto.report;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DailyRevenueReportDto {
    private String branchName;
    private String branchAddress;
    private String fromDate;
    private String toDate;
    private String generatedAt;
    private String totalOrders;
    private String grossRevenue;
    private String totalDiscount;
    private String netRevenue;
    private List<DailyRevenueReportItemDto> items;
}
