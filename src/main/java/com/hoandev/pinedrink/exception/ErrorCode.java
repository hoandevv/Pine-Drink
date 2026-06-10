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
    AUTH_025("AUTH_025", "Local password is already set"),
    AUTH_026("AUTH_026", "Local password is not set"),
    AUTH_027("AUTH_027", "Passwords do not match"),
    AUTH_GOOGLE_001("AUTH_GOOGLE_001", "Google ID token is required"),
    AUTH_GOOGLE_002("AUTH_GOOGLE_002", "Google ID token is invalid"),
    AUTH_GOOGLE_003("AUTH_GOOGLE_003", "Google email is not verified"),
    AUTH_GOOGLE_004("AUTH_GOOGLE_004", "Email is already used by another provider"),
    AUTH_GOOGLE_005("AUTH_GOOGLE_005", "Google account does not match existing account"),

    RATE_LIMIT_EXCEEDED("RATE_001", "Rate limit exceeded - too many requests"),

    ROLE_NOT_FOUND("ROLE_001", "Role not found"),
    SCOPE_NOT_FOUND("SCOPE_001", "Scope not found"),

    CUSTOMER_001("CUSTOMER_001", "Customer profile not found"),
    CUSTOMER_002("CUSTOMER_002", "Customer address not found"),
    CUSTOMER_003("CUSTOMER_003", "Customer address does not belong to this customer"),
    CUSTOMER_004("CUSTOMER_004", "Cannot delete default address"),
    CUSTOMER_005("CUSTOMER_005", "Customer already has a default address"),

    BRANCH_001("BRANCH_001", "Branch not found"),
    BRANCH_002("BRANCH_002", "Branch code already exists"),
    BRANCH_003("BRANCH_003", "Branch scope not found"),
    BRANCH_004("BRANCH_004", "Branch is already inactive"),
    BRANCH_005("BRANCH_005", "Branch hours not found"),
    BRANCH_006("BRANCH_006", "Branch hours already exists for this day"),
    BRANCH_007("BRANCH_007", "Open time must be before close time"),
    BRANCH_008("BRANCH_008", "Branch product availability not found"),
    BRANCH_009("BRANCH_009", "Branch product availability already exists"),
    BRANCH_010("BRANCH_010", "Branch topping availability not found"),
    BRANCH_011("BRANCH_011", "Branch topping availability already exists"),
    BRANCH_012("BRANCH_012", "Available from must be before available to"),

    CATEGORY_001("CATEGORY_001", "Category not found"),
    CATEGORY_002("CATEGORY_002", "Category code already exists"),
    CATEGORY_003("CATEGORY_003", "Category is already inactive"),

    TOPPING_001("TOPPING_001", "Topping not found"),
    TOPPING_002("TOPPING_002", "Topping code already exists"),
    TOPPING_003("TOPPING_003", "Topping is already inactive"),
    TOPPING_004("TOPPING_004", "Product topping not found"),
    TOPPING_005("TOPPING_005", "Product topping already exists"),
    TOPPING_006("TOPPING_006", "Product topping is already inactive"),
    TOPPING_007("TOPPING_007", "Product topping does not belong to product"),

    PRODUCT_001("PRODUCT_001", "Product not found"),
    PRODUCT_002("PRODUCT_002", "Product code already exists"),
    PRODUCT_003("PRODUCT_003", "Product scope not found"),
    PRODUCT_004("PRODUCT_004", "Category not found"),
    PRODUCT_005("PRODUCT_005", "Product is already inactive"),
    PRODUCT_006("PRODUCT_006", "Category does not match product scope"),
    PRODUCT_007("PRODUCT_007", "Product variant not found"),
    PRODUCT_008("PRODUCT_008", "Product variant code already exists"),
    PRODUCT_009("PRODUCT_009", "Product variant is already inactive"),
    PRODUCT_010("PRODUCT_010", "Product variant does not belong to product"),

    VOUCHER_001("VOUCHER_001", "Voucher not found"),
    VOUCHER_002("VOUCHER_002", "Voucher code already exists"),
    VOUCHER_003("VOUCHER_003", "Voucher date range is invalid"),
    VOUCHER_004("VOUCHER_004", "Voucher discount rule is invalid"),
    VOUCHER_005("VOUCHER_005", "Voucher cannot be deleted because it has usage history"),
    VOUCHER_006("VOUCHER_006", "Voucher status is invalid"),
    VOUCHER_007("VOUCHER_007", "Voucher branch scope is invalid"),

    CHAT_001("CHAT_001", "Chat room not found"),
    CHAT_002("CHAT_002", "You do not have access to this chat room"),
    CHAT_003("CHAT_003", "Chat message content is required"),
    CHAT_004("CHAT_004", "Chat sender is invalid");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
