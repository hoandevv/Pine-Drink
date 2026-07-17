package com.hoandev.pinedrink.entity.dto.response.Voucher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherResponse {

    private String id;
    private String code;
    private String name;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private Integer usageLimit;
    private int usedCount;
    private Integer usageLimitPerCustomer;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String status;
    private List<String> branchIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
