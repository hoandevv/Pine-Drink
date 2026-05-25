package com.hoandev.pinedrink.entity.enums;

public enum SugarLevel implements BaseEnum {
    NORMAL("NORMAL"),
    LESS("LESS"),
    HALF("HALF"),
    FREE("FREE"),
    EXTRA("EXTRA");

    private final String value;

    SugarLevel(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
