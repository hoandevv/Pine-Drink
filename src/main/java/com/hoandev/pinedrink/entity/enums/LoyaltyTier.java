package com.hoandev.pinedrink.entity.enums;

public enum LoyaltyTier implements BaseEnum {
    SILVER("SILVER"),
    GOLD("GOLD"),
    PLATINUM("PLATINUM"),
    DIAMOND("DIAMOND");

    private final String value;

    LoyaltyTier(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
