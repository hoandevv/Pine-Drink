package com.hoandev.pinedrink.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ErrorResponse {
    private boolean success;
    private String errorCode;
    private String message;
    private Object errors;
    private Integer retryAfter;
    private String traceId;
    private LocalDateTime timestamp;

    public ErrorResponse(boolean success, String message, Object errors, Integer retryAfter, String traceId, LocalDateTime timestamp) {
        this.success = success;
        this.message = message;
        this.errors = errors;
        this.retryAfter = retryAfter;
        this.traceId = traceId;
        this.timestamp = timestamp;
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(false, errorCode.getMessage(), null, null, null, LocalDateTime.now());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(false, message, null, null, null, LocalDateTime.now());
    }

    @Getter
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
    }
}
