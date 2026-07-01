package com.hoandev.pinedrink.entity.enums;

public enum ReportType implements BaseEnum {
    INVOICE("INVOICE"),
    DAILY_REVENUE("DAILY_REVENUE"),
    PRODUCT_CATALOG("PRODUCT_CATALOG");

    private final String value;

    ReportType(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
