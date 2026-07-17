package com.hoandev.pinedrink.entity.enums;

public enum DiscountType implements BaseEnum {
    PERCENTAGE("PERCENTAGE"),
    FIXED_AMOUNT("FIXED_AMOUNT");

    private final String value;

    DiscountType(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
