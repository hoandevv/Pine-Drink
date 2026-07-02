package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.dto.report.DailyRevenueReportDto;
import com.hoandev.pinedrink.entity.dto.report.DailyRevenueReportItemDto;
import com.hoandev.pinedrink.repository.result.DailyRevenuePaymentResult;
import com.hoandev.pinedrink.repository.result.DailyRevenueSummaryResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class DailyRevenueReportMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat VND_FORMATTER = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));

    public DailyRevenueReportDto toReportDto(
            DailyRevenueSummaryResult summary,
            List<DailyRevenuePaymentResult> payments,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return DailyRevenueReportDto.builder()
                .branchName(summary.getBranchName())
                .branchAddress(summary.getBranchAddress())
                .fromDate(fromDate.format(DATE_FORMATTER))
                .toDate(toDate.format(DATE_FORMATTER))
                .generatedAt(LocalDateTime.now().format(DATE_TIME_FORMATTER))
                .totalOrders(String.valueOf(nullToZero(summary.getTotalOrders())))
                .grossRevenue(formatVnd(summary.getGrossRevenue()))
                .totalDiscount(formatVnd(summary.getTotalDiscount()))
                .netRevenue(formatVnd(summary.getNetRevenue()))
                .items(payments.stream()
                        .map(this::toItemDto)
                        .toList())
                .build();
    }

    private DailyRevenueReportItemDto toItemDto(DailyRevenuePaymentResult payment) {
        return DailyRevenueReportItemDto.builder()
                .paymentMethod(resolvePaymentMethod(payment.getPaymentMethod()))
                .orderCount(nullToZero(payment.getOrderCount()))
                .grossAmount(formatVnd(payment.getGrossAmount()))
                .discountAmount(formatVnd(payment.getDiscountAmount()))
                .netAmount(formatVnd(payment.getNetAmount()))
                .build();
    }

    private String resolvePaymentMethod(String paymentMethod) {
        return paymentMethod == null || paymentMethod.isBlank() ? "UNKNOWN" : paymentMethod;
    }

    private Long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private String formatVnd(BigDecimal amount) {
        return VND_FORMATTER.format(amount == null ? BigDecimal.ZERO : amount) + " VND";
    }
}
