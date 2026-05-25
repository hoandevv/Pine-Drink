package com.hoandev.pinedrink.entity.enums;

public enum MovementType implements BaseEnum {
    INBOUND("INBOUND"),
    OUTBOUND("OUTBOUND"),
    ADJUSTMENT("ADJUSTMENT"),
    RETURN("RETURN"),
    WRITE_OFF("WRITE_OFF");

    private final String value;

    MovementType(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
