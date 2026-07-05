package com.hoandev.pinedrink.entity.enums;

public enum ExportRequestStatus implements BaseEnum {
    PENDING("PENDING"),
    RUNNING("RUNNING"),
    DONE("DONE"),
    FAILED("FAILED"),
    CANCELLED("CANCELLED");

    private final String value;

    ExportRequestStatus(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
