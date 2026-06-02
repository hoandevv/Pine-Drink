package com.hoandev.pinedrink.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Central exception handler that translates exceptions into consistent
 * {@link ErrorResponse} bodies across all controllers.
 * <p>
 * Each handler method maps a specific exception type to the appropriate
 * HTTP status and {@link ErrorCode}, producing a uniform error contract
 * for API consumers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles {@code @Valid} validation failures from request body DTOs.
     * Collects all field-level constraint violations into the response.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, ErrorCode.COM_001, "Validation failed", errors, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleInvalidRequestBody(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.COM_001, "Invalid request body", null, null);
    }

    /**
     * Handles explicit bad-request scenarios raised by the application.
     */
    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage(), null, null);
    }

    /**
     * Handles all other application-level exceptions.
     * The HTTP status is resolved dynamically from the error code prefix.
     */
    @ExceptionHandler(BaseException.class)
    ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        HttpStatus status = resolveHttpStatus(ex.getErrorCode());
        return build(status, ex.getErrorCode(), ex.getMessage(), null, null);
    }

    /**
     * Handles Spring Security authentication failures.
     */
    @ExceptionHandler({AuthenticationException.class})
    ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_001, ex.getMessage(), null, null);
    }

    /**
     * Handles authorization failures (insufficient permissions).
     */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ErrorCode.AUTH_007, "Forbidden", null, null);
    }

    /**
     * Fallback handler for any unhandled exception.
     * Returns a generic 500 Internal Server Error without exposing internals.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleException(Exception ex) {
        // Ignore static resource not found exceptions (favicon, etc.)
        if (ex instanceof org.springframework.web.servlet.resource.NoResourceFoundException) {
            return null; // Let Spring handle it with 404
        }
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.COM_002, "Internal server error", null, null);
    }

    private HttpStatus resolveHttpStatus(ErrorCode errorCode) {
        if (errorCode == null) return HttpStatus.BAD_REQUEST;
        String code = errorCode.getCode();
        if (code.startsWith("AUTH_")) {
            if (code.equals("AUTH_007")) return HttpStatus.FORBIDDEN;
            if (code.equals("AUTH_008")) return HttpStatus.TOO_MANY_REQUESTS;
            return HttpStatus.UNAUTHORIZED;
        }
        if (code.equals("COM_005") || code.equals("ROLE_001") || code.equals("TIMESLOT_NOT_FOUND")
                || code.equals("ROOM_001") || code.equals("TEACHER_001")) {
            return HttpStatus.NOT_FOUND;
        }
        if (code.equals("PRODUCT_001") || code.equals("PRODUCT_003") || code.equals("PRODUCT_004") || code.equals("PRODUCT_007")) {
            return HttpStatus.NOT_FOUND;
        }
        if (code.equals("CATEGORY_001")) {
            return HttpStatus.NOT_FOUND;
        }
        if (code.equals("RATE_001")) return HttpStatus.TOO_MANY_REQUESTS;
        if (code.startsWith("CFG_")) return HttpStatus.CONFLICT;
        if (code.equals("COM_002")) return HttpStatus.INTERNAL_SERVER_ERROR;
        return HttpStatus.BAD_REQUEST;
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, ErrorCode errorCode, String message,
                                                List<ErrorResponse.FieldError> errors, Integer retryAfter) {
        ErrorResponse body = new ErrorResponse(false, message, errors, retryAfter, null, LocalDateTime.now());
        body.setErrorCode(errorCode != null ? errorCode.getCode() : null);
        return ResponseEntity.status(status).body(body);
    }
}
