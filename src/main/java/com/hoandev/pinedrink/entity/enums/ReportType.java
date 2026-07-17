package com.hoandev.pinedrink.entity.enums;

public enum ReportType implements BaseEnum {
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
