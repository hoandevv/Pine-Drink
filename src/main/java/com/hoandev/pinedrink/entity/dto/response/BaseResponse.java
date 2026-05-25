package com.hoandev.pinedrink.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Standard wrapper for API success and error responses.
 *
 * @param <T> response data type
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BaseResponse<T> {

    private boolean success;
    private String errorCode;
    private String message;
    private T data;
    private List<FieldError> fieldErrors;
    private Instant timestamp;

    /**
     * Creates a successful response with the default message.
     */
    public static <T> BaseResponse<T> success(T data) {
        return BaseResponse.<T>builder()
                .success(true)
                .message("Success")
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates a successful response with a custom message.
     */
    public static <T> BaseResponse<T> success(T data, String message) {
        return BaseResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates an error response without field-level validation details.
     */
    public static <T> BaseResponse<T> error(String errorCode, String message) {
        return error(errorCode, message, null);
    }

    /**
     * Creates an error response with optional field-level validation details.
     */
    public static <T> BaseResponse<T> error(String errorCode, String message, List<FieldError> fieldErrors) {
        return BaseResponse.<T>builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .fieldErrors(fieldErrors)
                .timestamp(Instant.now())
                .build();
    }
}
