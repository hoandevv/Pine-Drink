package com.hoandev.pinedrink.entity.dto.response.Payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomoIpnResponse {
    private String partnerCode;
    private String orderId;
    private String requestId;
    private Integer resultCode;
    private String message;
}
