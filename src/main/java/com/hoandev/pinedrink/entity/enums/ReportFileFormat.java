package com.hoandev.pinedrink.entity.enums;

public enum ReportFileFormat implements BaseEnum {
    XLSX("XLSX"),
    CSV("CSV"),
    PDF("PDF");

    private final String value;

    ReportFileFormat(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
