package com.hoandev.pinedrink.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BranchRequest {
    @NotBlank
    private String brandId;

    @NotBlank
    private String code;

    @NotBlank
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
}
