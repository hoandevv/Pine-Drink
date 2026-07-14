package com.hoandev.pinedrink.entity.dto.response.Dashboard;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDataResponse {
    private DashboardOverviewResponse overview;
    private List<RevenueTrendResponse> revenueTrend;
    private List<OrderStatusSummaryResponse> orderStatus;
    private List<TopProductResponse> topProducts;
    private List<BranchPerformanceResponse> branchPerformance;
}
