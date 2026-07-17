package com.hoandev.pinedrink.entity.dto.response.Product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSummaryResponse {
    private String id;
    private String code;
    private String name;
    private String imageUrl;
    private BigDecimal basePrice;
    private String categoryName;
    private String status;
}
