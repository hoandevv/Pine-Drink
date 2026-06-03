package com.hoandev.pinedrink.entity.dto.response.Product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantResponse {

    private String id;
    private String productId;
    private String productCode;
    private String productName;
    private String variantCode;
    private String variantName;
    private String sizeLabel;
    private BigDecimal priceDelta;
    private BigDecimal finalPrice;
    private int displayOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
