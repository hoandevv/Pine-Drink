package com.hoandev.pinedrink.entity.enums;

public enum ToppingStatus implements BaseEnum {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE");

    private final String value;

    ToppingStatus(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
