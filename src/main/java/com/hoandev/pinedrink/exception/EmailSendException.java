package com.hoandev.pinedrink.exception;

/**
 * Exception thrown when an email fails to send after being queued.
 * <p>
 * This is a non-retryable runtime exception — the message has already
 * been consumed from the queue and delivery failure is logged for
 * manual investigation.
 */
public class EmailSendException extends RuntimeException {
    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
