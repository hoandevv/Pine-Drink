package com.hoandev.pinedrink.entity.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Standard wrapper for API success and error responses.
 *
 * @param <T> response data type
 */
public class BaseResponse<T> {

    private boolean success;
    private String errorCode;
    private String message;
    private T data;
    private List<FieldError> fieldErrors;
    private Instant timestamp;

    public BaseResponse() {
    }

    public BaseResponse(boolean success, String errorCode, String message, T data,
                        List<FieldError> fieldErrors, Instant timestamp) {
        this.success = success;
        this.errorCode = errorCode;
        this.message = message;
        this.data = data;
        this.fieldErrors = fieldErrors;
        this.timestamp = timestamp;
    }

    /**
     * Creates a successful response with the default message.
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(true, null, "Success", data, null, Instant.now());
    }

    /**
     * Creates a successful response with a custom message.
     */
    public static <T> BaseResponse<T> success(T data, String message) {
        return new BaseResponse<>(true, null, message, data, null, Instant.now());
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
        return new BaseResponse<>(false, errorCode, message, null, fieldErrors, Instant.now());
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(List<FieldError> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
