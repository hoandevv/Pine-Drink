package com.hoandev.pinedrink.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    COM_001("COM_001", "Validation error"),
    COM_002("COM_002", "Internal server error"),
    COM_003("COM_003", "Malformed JSON request"),
    COM_004("COM_004", "Invalid request parameter"),
    COM_005("COM_005", "Resource not found"),

    AUTH_001("AUTH_001", "Invalid username or password"),
    AUTH_002("AUTH_002", "Token expired"),
    AUTH_003("AUTH_003", "Invalid token"),
    AUTH_004("AUTH_004", "Refresh token is invalid or expired"),
    AUTH_005("AUTH_005", "Account is locked"),
    AUTH_006("AUTH_006", "Account is inactive"),
    AUTH_007("AUTH_007", "Insufficient permissions"),
    AUTH_008("AUTH_008", "Too many requests"),
    AUTH_009("AUTH_009", "Rate limit service unavailable"),
    AUTH_010("AUTH_010", "Weak password"),
    AUTH_011("AUTH_011", "Password reset token expired"),
    AUTH_012("AUTH_012", "Account not found"),
    AUTH_013("AUTH_013", "Username already exists"),
    AUTH_014("AUTH_014", "Email already exists"),
    AUTH_015("AUTH_015", "Phone already exists"),
    AUTH_016("AUTH_016", "OTP is invalid"),
    AUTH_017("AUTH_017", "OTP is expired"),
    AUTH_018("AUTH_018", "Account is already active"),
    AUTH_019("AUTH_019", "Too many invalid OTP attempts"),
    AUTH_020("AUTH_020", "Please wait before requesting another OTP"),
    AUTH_021("AUTH_021", "Account is not assigned to an active center"),
    AUTH_022("AUTH_022", "Invalid registration site"),
    AUTH_023("AUTH_023", "Public registration is disabled for this site"),
    AUTH_024("AUTH_024", "Center domain already exists"),

    RATE_LIMIT_EXCEEDED("RATE_001", "Rate limit exceeded - too many requests"),

    ROLE_NOT_FOUND("ROLE_001", "Role not found");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
