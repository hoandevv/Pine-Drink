package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.Dashboard.DashboardDataResponse;
import com.hoandev.pinedrink.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/getAllData")
    @PreAuthorize("hasAuthority('PERM_REPORT_VIEW')")
    public ResponseEntity<BaseResponse<DashboardDataResponse>> getAllData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(BaseResponse.success(
                dashboardService.getAllData(fromDate, toDate, branchId, limit),
                "Dashboard data retrieved successfully"));
    }
}
