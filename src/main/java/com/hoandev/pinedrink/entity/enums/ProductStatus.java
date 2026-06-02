package com.hoandev.pinedrink.entity.enums;

public enum ProductStatus implements BaseEnum {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    OUT_OF_STOCK("OUT_OF_STOCK");

    private final String value;

    ProductStatus(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
