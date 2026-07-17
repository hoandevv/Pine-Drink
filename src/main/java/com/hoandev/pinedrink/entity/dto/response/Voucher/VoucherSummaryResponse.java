package com.hoandev.pinedrink.entity.dto.response.Voucher;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class VoucherSummaryResponse {
    private String id;
    private String code;
    private String name;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private Integer usageLimit;
    private int usedCount;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String status;
}
