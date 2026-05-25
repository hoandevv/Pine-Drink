package com.hoandev.pinedrink.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BranchResponse {
    private String id;
    private String brandId;
    private String brandName;
    private String code;
    private String name;
    private String address;
    private String phone;
    private String email;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String timezone;
    private boolean supportsPickup;
    private boolean supportsDelivery;
    private int averagePreparationMinutes;
    private String status;
    private LocalDateTime createdAt;
}
