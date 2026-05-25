package com.hoandev.pinedrink.entity.enums;

public enum RoleType implements BaseEnum {
    SYSTEM("SYSTEM"),
    BRAND("BRAND"),
    BRANCH("BRANCH");

    private final String value;

    RoleType(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
