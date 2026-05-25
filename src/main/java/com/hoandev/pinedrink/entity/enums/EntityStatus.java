package com.hoandev.pinedrink.entity.enums;

public enum EntityStatus implements BaseEnum {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    DELETED("DELETED");

    private final String value;

    EntityStatus(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
