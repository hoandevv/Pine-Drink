package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.dto.response.Dashboard.DashboardDataResponse;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.repository.DashboardRepository;
import com.hoandev.pinedrink.security.scope.AccessScopeContext;
import com.hoandev.pinedrink.service.AccessScopeService;
import com.hoandev.pinedrink.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final int DEFAULT_TOP_PRODUCT_LIMIT = 10;
    private static final int MAX_TOP_PRODUCT_LIMIT = 50;
    private static final long MAX_RANGE_DAYS = 366;

    private final DashboardRepository dashboardRepository;
    private final AccessScopeService accessScopeService;

    @Override
    @Transactional(readOnly = true)
    public DashboardDataResponse getAllData(LocalDate fromDate, LocalDate toDate, String branchId, Integer limit) {
        validateDateRange(fromDate, toDate);
        String allowedBranchId = resolveBranchFilter(branchId);
        int normalizedLimit = normalizeLimit(limit);

        DashboardDataResponse.DashboardDataResponseBuilder builder = DashboardDataResponse.builder()
                .overview(dashboardRepository.getOverview(fromDate, toDate, allowedBranchId))
                .revenueTrend(dashboardRepository.getRevenueTrend(fromDate, toDate, allowedBranchId))
                .orderStatus(dashboardRepository.getOrderStatus(fromDate, toDate, allowedBranchId))
                .topProducts(dashboardRepository.getTopProducts(fromDate, toDate, allowedBranchId, normalizedLimit));

        if (allowedBranchId == null) {
            builder.branchPerformance(dashboardRepository.getBranchPerformance(fromDate, toDate));
        }

        return builder.build();
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new BaseException(ErrorCode.COM_004, "fromDate và toDate là bắt buộc");
        }
        if (fromDate.isAfter(toDate)) {
            throw new BaseException(ErrorCode.COM_004, "fromDate phải nhỏ hơn hoặc bằng toDate");
        }
        if (ChronoUnit.DAYS.between(fromDate, toDate) > MAX_RANGE_DAYS) {
            throw new BaseException(ErrorCode.COM_004, "Khoảng ngày không được vượt quá 366 ngày");
        }
    }

    private String resolveBranchFilter(String branchId) {
        String normalizedBranchId = normalizeNullable(branchId);
        AccessScopeContext scope = accessScopeService.resolveCurrentScope();
        if (scope.fullAccess()) {
            return normalizedBranchId;
        }
        if (normalizedBranchId == null) {
            throw new BaseException(ErrorCode.AUTH_007, "branchId là bắt buộc với người dùng thuộc phạm vi chi nhánh");
        }
        if (!scope.branchIds().contains(normalizedBranchId)) {
            throw new BaseException(ErrorCode.AUTH_007);
        }
        return normalizedBranchId;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_TOP_PRODUCT_LIMIT;
        }
        if (limit < 1 || limit > MAX_TOP_PRODUCT_LIMIT) {
            throw new BaseException(ErrorCode.COM_004, "limit phải nằm trong khoảng 1 đến 50");
        }
        return limit;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
