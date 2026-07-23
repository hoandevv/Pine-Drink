package com.hoandev.pinedrink.entity.enums;

public enum BranchStatus implements BaseEnum {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE");

    private final String value;

    BranchStatus(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
