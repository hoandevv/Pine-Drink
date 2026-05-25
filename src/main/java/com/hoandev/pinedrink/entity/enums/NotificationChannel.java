package com.hoandev.pinedrink.entity.enums;

public enum NotificationChannel implements BaseEnum {
    IN_APP("IN_APP"),
    EMAIL("EMAIL"),
    SMS("SMS"),
    PUSH("PUSH");

    private final String value;

    NotificationChannel(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
