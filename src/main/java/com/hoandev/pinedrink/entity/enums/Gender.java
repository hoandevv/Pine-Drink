package com.hoandev.pinedrink.entity.enums;

public enum Gender implements BaseEnum {
    MALE("MALE"),
    FEMALE("FEMALE"),
    OTHER("OTHER");

    private final String value;

    Gender(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
