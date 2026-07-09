package com.hoandev.pinedrink.repository.custom.Impl;

import com.hoandev.pinedrink.entity.dto.response.Dashboard.BranchPerformanceResponse;
import com.hoandev.pinedrink.entity.dto.response.Dashboard.DashboardOverviewResponse;
import com.hoandev.pinedrink.entity.dto.response.Dashboard.OrderStatusSummaryResponse;
import com.hoandev.pinedrink.entity.dto.response.Dashboard.RevenueTrendResponse;
import com.hoandev.pinedrink.entity.dto.response.Dashboard.TopProductResponse;
import com.hoandev.pinedrink.repository.DashboardRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Repository
public class DashboardRepositoryImpl implements DashboardRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public DashboardOverviewResponse getOverview(LocalDate fromDate, LocalDate toDate, String branchId) {
        Query query = entityManager.createNativeQuery("CALL sp_dashboard_overview(:fromDate, :toDate, :branchId)");
        setDateRangeAndBranch(query, fromDate, toDate, branchId);
        Object[] row = singleRow(query.getResultList());
        return DashboardOverviewResponse.builder()
                .totalRevenue(toBigDecimal(row[0]))
                .completedOrders(toLong(row[1]))
                .cancelledOrders(toLong(row[2]))
                .averageOrderValue(toBigDecimal(row[3]))
                .newCustomers(toLong(row[4]))
                .build();
    }

    @Override
    public List<RevenueTrendResponse> getRevenueTrend(LocalDate fromDate, LocalDate toDate, String branchId) {
        Query query = entityManager.createNativeQuery("CALL sp_dashboard_revenue_trend(:fromDate, :toDate, :branchId)");
        setDateRangeAndBranch(query, fromDate, toDate, branchId);
        return rows(query).stream()
                .map(row -> RevenueTrendResponse.builder()
                        .date(toLocalDate(row[0]))
                        .revenue(toBigDecimal(row[1]))
                        .orders(toLong(row[2]))
                        .averageOrderValue(toBigDecimal(row[3]))
                        .build())
                .toList();
    }

    @Override
    public List<OrderStatusSummaryResponse> getOrderStatus(LocalDate fromDate, LocalDate toDate, String branchId) {
        Query query = entityManager.createNativeQuery("CALL sp_dashboard_order_status(:fromDate, :toDate, :branchId)");
        setDateRangeAndBranch(query, fromDate, toDate, branchId);
        return rows(query).stream()
                .map(row -> OrderStatusSummaryResponse.builder()
                        .status((String) row[0])
                        .count(toLong(row[1]))
                        .build())
                .toList();
    }

    @Override
    public List<TopProductResponse> getTopProducts(LocalDate fromDate, LocalDate toDate, String branchId, int limit) {
        Query query = entityManager.createNativeQuery("CALL sp_dashboard_top_products(:fromDate, :toDate, :branchId, :limit)");
        setDateRangeAndBranch(query, fromDate, toDate, branchId);
        query.setParameter("limit", limit);
        return rows(query).stream()
                .map(row -> TopProductResponse.builder()
                        .productId((String) row[0])
                        .productName((String) row[1])
                        .quantitySold(toLong(row[2]))
                        .revenue(toBigDecimal(row[3]))
                        .build())
                .toList();
    }

    @Override
    public List<BranchPerformanceResponse> getBranchPerformance(LocalDate fromDate, LocalDate toDate) {
        Query query = entityManager.createNativeQuery("CALL sp_dashboard_branch_performance(:fromDate, :toDate)");
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        return rows(query).stream()
                .map(row -> BranchPerformanceResponse.builder()
                        .branchId((String) row[0])
                        .branchName((String) row[1])
                        .revenue(toBigDecimal(row[2]))
                        .completedOrders(toLong(row[3]))
                        .cancelledOrders(toLong(row[4]))
                        .averageOrderValue(toBigDecimal(row[5]))
                        .build())
                .toList();
    }

    private void setDateRangeAndBranch(Query query, LocalDate fromDate, LocalDate toDate, String branchId) {
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        query.setParameter("branchId", branchId);
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(Query query) {
        return query.getResultList();
    }

    private Object[] singleRow(List<?> resultList) {
        return resultList.isEmpty() ? new Object[]{BigDecimal.ZERO, 0, 0, BigDecimal.ZERO, 0} : (Object[]) resultList.get(0);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : Long.parseLong(value.toString());
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }
}
