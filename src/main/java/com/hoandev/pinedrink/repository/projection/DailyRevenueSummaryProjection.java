package com.hoandev.pinedrink.repository.projection;

import java.math.BigDecimal;

public interface DailyRevenueSummaryProjection {
    String getBranchName();
    String getBranchAddress();
    Long getTotalOrders();
    BigDecimal getGrossRevenue();
    BigDecimal getTotalDiscount();
    BigDecimal getNetRevenue();
}
