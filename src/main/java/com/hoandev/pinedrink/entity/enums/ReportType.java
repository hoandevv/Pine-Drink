package com.hoandev.pinedrink.entity.enums;

public enum ReportType implements BaseEnum {
    INVOICE("INVOICE");

    private final String value;

    ReportType(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
