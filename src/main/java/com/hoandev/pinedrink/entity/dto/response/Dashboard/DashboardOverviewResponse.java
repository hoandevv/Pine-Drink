package com.hoandev.pinedrink.entity.dto.response.Dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewResponse {
    private BigDecimal totalRevenue;
    private long completedOrders;
    private long cancelledOrders;
    private BigDecimal averageOrderValue;
    private long newCustomers;
}
