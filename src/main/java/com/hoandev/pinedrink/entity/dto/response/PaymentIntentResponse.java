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
public class PaymentIntentResponse {
    private String id;
    private String orderId;
    private String orderCode;
    private String provider;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDateTime expiresAt;
}
