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
public class ProductToppingResponse {

    private String id;
    private String productId;
    private String productCode;
    private String productName;
    private String toppingId;
    private String toppingCode;
    private String toppingName;
    private BigDecimal toppingPrice;
    private String toppingImageUrl;
    private String toppingGroupName;
    private boolean isDefault;
    private int maxQuantity;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
