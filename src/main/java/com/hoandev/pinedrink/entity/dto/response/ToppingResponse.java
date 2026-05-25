package com.hoandev.pinedrink.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ToppingResponse {
    private String id;
    private String brandId;
    private String code;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private String status;
}
