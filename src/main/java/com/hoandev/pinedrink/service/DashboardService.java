package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.response.Dashboard.BranchPerformanceResponse;
import com.hoandev.pinedrink.entity.dto.response.Dashboard.DashboardOverviewResponse;
import com.hoandev.pinedrink.entity.dto.response.Dashboard.OrderStatusSummaryResponse;
import com.hoandev.pinedrink.entity.dto.response.Dashboard.RevenueTrendResponse;
import com.hoandev.pinedrink.entity.dto.response.Dashboard.TopProductResponse;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {
    /**
     * Lấy dữ liệu tổng quan dashboard theo khoảng ngày và chi nhánh.
     *
     * @param fromDate ngày bắt đầu
     * @param toDate ngày kết thúc
     * @param branchId ID chi nhánh; null nghĩa là toàn hệ thống
     * @return dữ liệu tổng quan dashboard
     */
    DashboardOverviewResponse getOverview(LocalDate fromDate, LocalDate toDate, String branchId);

    /**
     * Lấy xu hướng doanh thu theo ngày trong khoảng thời gian và chi nhánh đã chọn.
     *
     * @param fromDate ngày bắt đầu
     * @param toDate ngày kết thúc
     * @param branchId ID chi nhánh; null nghĩa là toàn hệ thống
     * @return danh sách doanh thu theo ngày
     */
    List<RevenueTrendResponse> getRevenueTrend(LocalDate fromDate, LocalDate toDate, String branchId);

    /**
     * Lấy tổng hợp số lượng đơn theo trạng thái trong khoảng thời gian và chi nhánh đã chọn.
     *
     * @param fromDate ngày bắt đầu
     * @param toDate ngày kết thúc
     * @param branchId ID chi nhánh; null nghĩa là toàn hệ thống
     * @return danh sách số lượng đơn theo trạng thái
     */
    List<OrderStatusSummaryResponse> getOrderStatus(LocalDate fromDate, LocalDate toDate, String branchId);

    /**
     * Lấy danh sách sản phẩm bán chạy trong khoảng thời gian và chi nhánh đã chọn.
     *
     * @param fromDate ngày bắt đầu
     * @param toDate ngày kết thúc
     * @param branchId ID chi nhánh; null nghĩa là toàn hệ thống
     * @param limit số lượng sản phẩm tối đa cần trả về
     * @return danh sách sản phẩm bán chạy
     */
    List<TopProductResponse> getTopProducts(LocalDate fromDate, LocalDate toDate, String branchId, Integer limit);

    /**
     * Lấy hiệu suất của từng chi nhánh trong khoảng thời gian đã chọn.
     *
     * @param fromDate ngày bắt đầu
     * @param toDate ngày kết thúc
     * @return danh sách hiệu suất chi nhánh
     */
    List<BranchPerformanceResponse> getBranchPerformance(LocalDate fromDate, LocalDate toDate);
}
