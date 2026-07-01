package com.hoandev.pinedrink.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface InvoiceHeaderProjection {
    String getOrderId();

    String getOrderCode();

    String getCustomerName();

    BigDecimal getSubtotalAmount();

    BigDecimal getDiscountAmount();

    BigDecimal getTotalAmount();

    LocalDateTime getOrderTime();

    String getBranchName();

    String getBranchAddress();
}
