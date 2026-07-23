package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.dto.response.Dashboard.BranchPerformanceResponse;
import com.hoandev.pinedrink.entity.dto.response.Dashboard.DashboardOverviewResponse;
import com.hoandev.pinedrink.entity.dto.response.Dashboard.OrderStatusSummaryResponse;
import com.hoandev.pinedrink.entity.dto.response.Dashboard.RevenueTrendResponse;
import com.hoandev.pinedrink.entity.dto.response.Dashboard.TopProductResponse;

import java.time.LocalDate;
import java.util.List;

public interface DashboardRepository {

    /**
     * Calls {@code sp_dashboard_overview} from {@code V16__create_dashboard_procedures.sql}.
     * Returns summary KPIs for the selected date range and optional branch.
     * Revenue and average order value are calculated from completed orders only.
     *
     * @param fromDate inclusive start date
     * @param toDate inclusive end date
     * @param branchId optional branch filter; {@code null} means all branches
     * @return dashboard KPI summary
     */
    DashboardOverviewResponse getOverview(LocalDate fromDate, LocalDate toDate, String branchId);

    /**
     * Calls {@code sp_dashboard_revenue_trend} from {@code V16__create_dashboard_procedures.sql}.
     * Groups completed order revenue by day.
     *
     * @param fromDate inclusive start date
     * @param toDate inclusive end date
     * @param branchId optional branch filter; {@code null} means all branches
     * @return daily revenue trend rows ordered by date ascending
     */
    List<RevenueTrendResponse> getRevenueTrend(LocalDate fromDate, LocalDate toDate, String branchId);

    /**
     * Calls {@code sp_dashboard_order_status} from {@code V16__create_dashboard_procedures.sql}.
     * Counts orders by workflow status within the selected date range.
     *
     * @param fromDate inclusive start date
     * @param toDate inclusive end date
     * @param branchId optional branch filter; {@code null} means all branches
     * @return status/count pairs
     */
    List<OrderStatusSummaryResponse> getOrderStatus(LocalDate fromDate, LocalDate toDate, String branchId);

    /**
     * Calls {@code sp_dashboard_top_products} from {@code V16__create_dashboard_procedures.sql}.
     * Ranks products by sold quantity and revenue from completed orders.
     *
     * @param fromDate inclusive start date
     * @param toDate inclusive end date
     * @param branchId optional branch filter; {@code null} means all branches
     * @param limit maximum number of products to return
     * @return top-selling product rows
     */
    List<TopProductResponse> getTopProducts(LocalDate fromDate, LocalDate toDate, String branchId, int limit);

    /**
     * Calls {@code sp_dashboard_branch_performance} from {@code V16__create_dashboard_procedures.sql}.
     * Compares all branches by completed revenue, completed orders, cancelled orders, and average order value.
     *
     * @param fromDate inclusive start date
     * @param toDate inclusive end date
     * @return branch performance rows ordered by revenue descending
     */
    List<BranchPerformanceResponse> getBranchPerformance(LocalDate fromDate, LocalDate toDate);
}
