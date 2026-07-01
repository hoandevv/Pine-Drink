package com.hoandev.pinedrink.repository.projection;

import java.math.BigDecimal;

public interface InvoiceItemProjection {
    String getProductName();

    String getVariantName();

    Integer getQuantity();

    BigDecimal getUnitPrice();

    BigDecimal getLineTotal();
}
