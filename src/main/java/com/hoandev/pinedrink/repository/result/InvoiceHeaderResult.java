package com.hoandev.pinedrink.repository.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Getter
@RequiredArgsConstructor
public class InvoiceHeaderResult {
    private final String orderId;
    private final String orderCode;
    private final String customerName;
    private final BigDecimal subtotalAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal totalAmount;
    private final LocalDateTime orderTime;
    private final String branchName;
    private final String branchAddress;
}
