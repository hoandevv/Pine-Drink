package com.hoandev.pinedrink.entity.enums;

public enum OutboxStatus implements BaseEnum {
    PENDING("PENDING"),
    PROCESSING("PROCESSING"),
    PUBLISHED("PUBLISHED"),
    FAILED("FAILED");

    private final String value;

    OutboxStatus(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
