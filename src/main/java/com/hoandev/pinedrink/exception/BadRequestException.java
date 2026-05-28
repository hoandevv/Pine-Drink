package com.hoandev.pinedrink.exception;

/**
 * Exception indicating a malformed or invalid client request (HTTP 400).
 * <p>
 * Pairs with {@link ErrorCode} values that represent client-side input errors.
 */
public class BadRequestException extends BaseException {

    /**
     * Creates a bad-request exception using the default message from the error code.
     */
    public BadRequestException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * Creates a bad-request exception with a custom message.
     */
    public BadRequestException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
