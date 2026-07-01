package com.hoandev.pinedrink.entity.dto.report;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class InvoiceReportDto {
    private String branchName;
    private String branchAddress;
    private String orderCode;
    private String customerName;
    private String cashierName;
    private String orderTime;
    private String subtotal;
    private String discount;
    private String total;
    private List<InvoiceReportItemDto> items;
}
