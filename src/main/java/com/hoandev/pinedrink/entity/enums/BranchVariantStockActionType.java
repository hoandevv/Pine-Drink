package com.hoandev.pinedrink.entity.enums;

import lombok.Getter;

@Getter
public enum BranchVariantStockActionType {
    SET_QUOTA("SET_QUOTA"),
    COPY_QUOTA("COPY_QUOTA"),
    ADJUST("ADJUST"),
    RESERVE("RESERVE"),
    SOLD("SOLD"),
    RELEASE("RELEASE");

    private final String value;

    BranchVariantStockActionType(String value) {
        this.value = value;
    }
}
