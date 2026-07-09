package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.Dashboard.BranchPerformanceResponse;
import com.hoandev.pinedrink.entity.dto.response.Dashboard.DashboardOverviewResponse;
import com.hoandev.pinedrink.entity.dto.response.Dashboard.OrderStatusSummaryResponse;
import com.hoandev.pinedrink.entity.dto.response.Dashboard.RevenueTrendResponse;
import com.hoandev.pinedrink.entity.dto.response.Dashboard.TopProductResponse;
import com.hoandev.pinedrink.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public ResponseEntity<BaseResponse<DashboardOverviewResponse>> getOverview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String branchId) {
        return ResponseEntity.ok(BaseResponse.success(
                dashboardService.getOverview(fromDate, toDate, branchId),
                "Dashboard overview retrieved successfully"));
    }

    @GetMapping("/revenue-trend")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public ResponseEntity<BaseResponse<List<RevenueTrendResponse>>> getRevenueTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String branchId) {
        return ResponseEntity.ok(BaseResponse.success(
                dashboardService.getRevenueTrend(fromDate, toDate, branchId),
                "Revenue trend retrieved successfully"));
    }

    @GetMapping("/order-status")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public ResponseEntity<BaseResponse<List<OrderStatusSummaryResponse>>> getOrderStatus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String branchId) {
        return ResponseEntity.ok(BaseResponse.success(
                dashboardService.getOrderStatus(fromDate, toDate, branchId),
                "Order status summary retrieved successfully"));
    }

    @GetMapping("/top-products")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public ResponseEntity<BaseResponse<List<TopProductResponse>>> getTopProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(BaseResponse.success(
                dashboardService.getTopProducts(fromDate, toDate, branchId, limit),
                "Top products retrieved successfully"));
    }

    @GetMapping("/branch-performance")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public ResponseEntity<BaseResponse<List<BranchPerformanceResponse>>> getBranchPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(BaseResponse.success(
                dashboardService.getBranchPerformance(fromDate, toDate),
                "Branch performance retrieved successfully"));
    }
}
