package com.hoandev.pinedrink.entity.enums;

public enum ProductVariantStatus implements BaseEnum {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE");

    private final String value;

    ProductVariantStatus(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
