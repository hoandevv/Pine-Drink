package com.hoandev.pinedrink.repository.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class DailyRevenueSummaryResult {
    private final String branchName;
    private final String branchAddress;
    private final Long totalOrders;
    private final BigDecimal grossRevenue;
    private final BigDecimal totalDiscount;
    private final BigDecimal netRevenue;
}
