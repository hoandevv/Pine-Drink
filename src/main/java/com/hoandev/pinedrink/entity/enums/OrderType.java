package com.hoandev.pinedrink.entity.enums;

public enum OrderType implements BaseEnum {
    PICKUP("PICKUP"),
    DELIVERY("DELIVERY");

    private final String value;

    OrderType(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
