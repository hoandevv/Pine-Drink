package com.hoandev.pinedrink.entity.enums;

public enum PaymentStatus implements BaseEnum {
    UNPAID("UNPAID"),
    PAID("PAID"),
    REFUNDED("REFUNDED"),
    FAILED("FAILED"),
    PARTIALLY_PAID("PARTIALLY_PAID");

    private final String value;

    PaymentStatus(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
