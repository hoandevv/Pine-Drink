package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.dto.report.InvoiceReportDto;
import com.hoandev.pinedrink.entity.dto.report.InvoiceReportItemDto;
import com.hoandev.pinedrink.repository.projection.InvoiceHeaderProjection;
import com.hoandev.pinedrink.repository.projection.InvoiceItemProjection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class InvoiceReportMapper {

    private static final DateTimeFormatter INVOICE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat VND_FORMATTER = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));

    public InvoiceReportDto toReportDto(
            InvoiceHeaderProjection header,
            List<InvoiceItemProjection> items,
            String cashierName
    ) {
        return InvoiceReportDto.builder()
                .branchName(requireText(header.getBranchName(), "Invoice branch name is missing"))
                .branchAddress(header.getBranchAddress())
                .orderCode(header.getOrderCode())
                .customerName(header.getCustomerName())
                .cashierName(cashierName)
                .orderTime(header.getOrderTime().format(INVOICE_TIME_FORMATTER))
                .subtotal(formatVnd(header.getSubtotalAmount()))
                .discount(formatVnd(header.getDiscountAmount()))
                .total(formatVnd(header.getTotalAmount()))
                .items(items.stream()
                        .map(this::toInvoiceItem)
                        .toList())
                .build();
    }

    private InvoiceReportItemDto toInvoiceItem(InvoiceItemProjection item) {
        return InvoiceReportItemDto.builder()
                .productName(resolveProductName(item))
                .quantity(item.getQuantity())
                .unitPrice(formatVnd(item.getUnitPrice()))
                .lineTotal(formatVnd(item.getLineTotal()))
                .build();
    }

    private String resolveProductName(InvoiceItemProjection item) {
        if (item.getVariantName() == null || item.getVariantName().isBlank()) {
            return item.getProductName();
        }
        return item.getProductName() + " - " + item.getVariantName();
    }

    private String formatVnd(BigDecimal amount) {
        return VND_FORMATTER.format(amount == null ? BigDecimal.ZERO : amount) + " VND";
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
