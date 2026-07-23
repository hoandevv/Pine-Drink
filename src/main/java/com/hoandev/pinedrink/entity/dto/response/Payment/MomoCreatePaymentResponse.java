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
public class MomoCreatePaymentResponse {
    private String orderId;
    private String requestId;
    private String payUrl;
    private String deeplink;
    private String qrCodeUrl;
    private Integer resultCode;
    private String message;
    private String provider;
    private String paymentMethod;
    private String transactionId;
}
