package com.hoandev.pinedrink.entity.enums;

public enum CategoryStatus implements BaseEnum {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE");

    private final String value;

    CategoryStatus(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
