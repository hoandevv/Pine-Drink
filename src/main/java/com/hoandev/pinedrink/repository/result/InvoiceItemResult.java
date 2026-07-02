package com.hoandev.pinedrink.repository.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class InvoiceItemResult {
    private final String productName;
    private final String variantName;
    private final Integer quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal lineTotal;
}
