package com.hoandev.pinedrink.entity.enums;

public enum LoyaltyTransactionType implements BaseEnum {
    EARN("EARN"),
    REDEEM("REDEEM"),
    ADJUST("ADJUST"),
    EXPIRE("EXPIRE");

    private final String value;

    LoyaltyTransactionType(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
