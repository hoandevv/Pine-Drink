package com.hoandev.pinedrink.entity.dto.response.Branch;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchResponse {

    private String id;
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
    private String brandId;
    private String brandName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
