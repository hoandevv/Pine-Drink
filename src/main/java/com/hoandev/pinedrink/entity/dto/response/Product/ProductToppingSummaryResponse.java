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
public class ProductToppingSummaryResponse {

    private String id;
    private String productId;
    private String toppingId;
    private String toppingCode;
    private String toppingName;
    private BigDecimal toppingPrice;
    private String toppingImageUrl;
    private String toppingGroupName;
    private boolean isDefault;
    private int maxQuantity;
    private String status;
}
