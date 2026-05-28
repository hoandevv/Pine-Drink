package com.hoandev.pinedrink.exception;

import lombok.Getter;

/**
 * Base application exception carrying an {@link ErrorCode} for structured error handling.
 * <p>
 * Subclasses can customize the message while preserving the error code for
 * downstream mapping in {@link GlobalExceptionHandler}.
 */
@Getter
public class BaseException extends RuntimeException {
    private final ErrorCode errorCode;

    /**
     * Creates an exception using the default message from the error code.
     */
    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * Creates an exception with a custom message, overriding the error code default.
     */
    public BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
