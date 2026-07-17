package com.hoandev.pinedrink.entity.enums;

public enum PaymentProvider implements BaseEnum {
    CASH("CASH"),
    MOMO("MOMO"),
    VNPAY("VNPAY"),
    BANK_TRANSFER("BANK_TRANSFER"),
    VNPAY_QR("VNPAY_QR");

    private final String value;

    PaymentProvider(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
