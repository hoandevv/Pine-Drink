package com.hoandev.pinedrink.entity.dto.report;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InvoiceReportItemDto {
    private String productName;
    private Integer quantity;
    private String unitPrice;
    private String lineTotal;
}
