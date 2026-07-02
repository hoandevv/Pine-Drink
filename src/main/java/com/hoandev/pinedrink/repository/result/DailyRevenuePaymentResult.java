package com.hoandev.pinedrink.repository.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class DailyRevenuePaymentResult {
    private final String paymentMethod;
    private final Long orderCount;
    private final BigDecimal grossAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal netAmount;
}
