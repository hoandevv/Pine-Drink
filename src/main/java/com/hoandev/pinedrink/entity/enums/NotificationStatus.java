package com.hoandev.pinedrink.entity.enums;

public enum NotificationStatus implements BaseEnum {
    UNREAD("UNREAD"),
    READ("READ");

    private final String value;

    NotificationStatus(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
