package com.hoandev.pinedrink.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ProductCatalogProjection {
    String getProductCode();

    String getProductName();

    String getCategoryName();

    BigDecimal getBasePrice();

    String getStatus();

    Integer getPreparationMinutes();

    Boolean getFeatured();

    Boolean getBestSeller();

    String getVariants();

    LocalDateTime getCreatedAt();
}
