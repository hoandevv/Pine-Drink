package com.hoandev.pinedrink.repository.projection;

import java.math.BigDecimal;

public interface DailyRevenuePaymentProjection {
    String getPaymentMethod();
    Long getOrderCount();
    BigDecimal getGrossAmount();
    BigDecimal getDiscountAmount();
    BigDecimal getNetAmount();
}
