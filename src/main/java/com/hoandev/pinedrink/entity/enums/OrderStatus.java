package com.hoandev.pinedrink.entity.enums;

public enum OrderStatus implements BaseEnum {
    PENDING("PENDING"),
    CONFIRMED("CONFIRMED"),
    PREPARING("PREPARING"),
    READY("READY"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED");

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
