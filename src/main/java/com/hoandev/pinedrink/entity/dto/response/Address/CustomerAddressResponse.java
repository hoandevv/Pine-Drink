package com.hoandev.pinedrink.entity.dto.response.Address;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Builder
public class CustomerAddressResponse  {

    private String id;

    private String receiverName;

    private String receiverPhone;

    private String addressLine;

    private String ward;

    private String district;

    private String city;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private boolean isDefault;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
