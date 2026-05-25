package com.hoandev.pinedrink.entity.enums;

public enum IdempotencyStatus implements BaseEnum {
    PROCESSING("PROCESSING"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    private final String value;

    IdempotencyStatus(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
