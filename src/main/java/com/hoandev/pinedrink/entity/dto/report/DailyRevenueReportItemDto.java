package com.hoandev.pinedrink.entity.dto.report;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DailyRevenueReportItemDto {
    private String paymentMethod;
    private Long orderCount;
    private String grossAmount;
    private String discountAmount;
    private String netAmount;
}
