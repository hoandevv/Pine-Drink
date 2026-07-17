package com.hoandev.pinedrink.entity.enums;

public enum ProductToppingStatus implements BaseEnum {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE");

    private final String value;

    ProductToppingStatus(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
