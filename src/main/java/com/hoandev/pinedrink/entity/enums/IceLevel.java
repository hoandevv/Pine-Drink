package com.hoandev.pinedrink.entity.enums;

public enum IceLevel implements BaseEnum {
    NORMAL("NORMAL"),
    LESS("LESS"),
    FREE("FREE"),
    EXTRA("EXTRA");

    private final String value;

    IceLevel(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
