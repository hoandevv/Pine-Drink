package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.Voucher;
import com.hoandev.pinedrink.entity.dto.request.Voucher.CreateVoucherRequest;
import com.hoandev.pinedrink.entity.dto.request.Voucher.UpdateVoucherRequest;
import com.hoandev.pinedrink.entity.dto.response.Voucher.VoucherResponse;
import com.hoandev.pinedrink.entity.dto.response.Voucher.VoucherSummaryResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class VoucherMapper {

    public Voucher toEntity(CreateVoucherRequest request, String normalizedCode) {
        if (request == null) {
            return null;
        }

        Voucher voucher = new Voucher();
        voucher.setCode(normalizedCode);
        voucher.setName(trimToNull(request.getName()));
        voucher.setDescription(trimToNull(request.getDescription()));
        voucher.setDiscountType(request.getDiscountType().getValue());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMaxDiscountAmount(resolveMaxDiscountAmount(request.getDiscountType().getValue(), request.getMaxDiscountAmount()));
        voucher.setMinOrderAmount(defaultMinOrderAmount(request.getMinOrderAmount()));
        voucher.setUsageLimit(request.getUsageLimit());
        voucher.setUsageLimitPerCustomer(request.getUsageLimitPerCustomer());
        voucher.setStartAt(request.getStartAt());
        voucher.setEndAt(request.getEndAt());
        return voucher;
    }

    public void updateEntity(Voucher voucher, UpdateVoucherRequest request, String normalizedCode) {
        if (voucher == null || request == null) {
            return;
        }

        voucher.setCode(normalizedCode);
        voucher.setName(trimToNull(request.getName()));
        voucher.setDescription(trimToNull(request.getDescription()));
        voucher.setDiscountType(request.getDiscountType().getValue());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMaxDiscountAmount(resolveMaxDiscountAmount(request.getDiscountType().getValue(), request.getMaxDiscountAmount()));
        voucher.setMinOrderAmount(defaultMinOrderAmount(request.getMinOrderAmount()));
        voucher.setUsageLimit(request.getUsageLimit());
        voucher.setUsageLimitPerCustomer(request.getUsageLimitPerCustomer());
        voucher.setStartAt(request.getStartAt());
        voucher.setEndAt(request.getEndAt());
    }

    public VoucherResponse toResponse(Voucher voucher, List<String> branchIds) {
        if (voucher == null) {
            return null;
        }

        return VoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .name(voucher.getName())
                .description(voucher.getDescription())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .minOrderAmount(voucher.getMinOrderAmount())
                .usageLimit(voucher.getUsageLimit())
                .usedCount(voucher.getUsedCount())
                .usageLimitPerCustomer(voucher.getUsageLimitPerCustomer())
                .startAt(voucher.getStartAt())
                .endAt(voucher.getEndAt())
                .status(voucher.getStatus())
                .branchIds(branchIds)
                .createdAt(voucher.getCreatedAt())
                .updatedAt(voucher.getUpdatedAt())
                .build();
    }

    public VoucherSummaryResponse toSummaryResponse(Voucher voucher) {
        if (voucher == null) {
            return null;
        }

        return VoucherSummaryResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .name(voucher.getName())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .minOrderAmount(voucher.getMinOrderAmount())
                .usageLimit(voucher.getUsageLimit())
                .usedCount(voucher.getUsedCount())
                .startAt(voucher.getStartAt())
                .endAt(voucher.getEndAt())
                .status(voucher.getStatus())
                .build();
    }

    private BigDecimal defaultMinOrderAmount(BigDecimal minOrderAmount) {
        return minOrderAmount != null ? minOrderAmount : BigDecimal.ZERO;
    }

    private BigDecimal resolveMaxDiscountAmount(String discountType, BigDecimal maxDiscountAmount) {
        return "PERCENTAGE".equals(discountType) ? maxDiscountAmount : null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
