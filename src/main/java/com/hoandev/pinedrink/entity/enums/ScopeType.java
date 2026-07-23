package com.hoandev.pinedrink.entity.enums;

public enum ScopeType implements BaseEnum {
    SYSTEM("SYSTEM"),
    BRAND("BRAND"),
    BRANCH("BRANCH");

    private final String value;

    ScopeType(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
