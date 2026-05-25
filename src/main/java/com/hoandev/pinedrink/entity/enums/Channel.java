package com.hoandev.pinedrink.entity.enums;

public enum Channel implements BaseEnum {
    WEB("WEB"),
    MOBILE("MOBILE");

    private final String value;

    Channel(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
