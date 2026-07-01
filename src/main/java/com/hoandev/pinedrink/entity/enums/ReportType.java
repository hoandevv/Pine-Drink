package com.hoandev.pinedrink.entity.enums;

public enum ReportType implements BaseEnum {
    INVOICE("INVOICE"),
    DAILY_REVENUE("DAILY_REVENUE");

    private final String value;

    ReportType(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
