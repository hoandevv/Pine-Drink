package com.hoandev.pinedrink.entity.dto.response.Product;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ToppingSummaryResponse {
    private String id;
    private String code;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private String groupName;
    private String status;
}
