package com.hoandev.pinedrink.entity.dto.response.Payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionResponse {
    private String id;
    private String transactionCode;
    private String orderId;
    private String orderCode;
    private String provider;
    private String paymentMethod;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String orderPaymentStatus;
    private LocalDateTime paidAt;
    private String failedReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
