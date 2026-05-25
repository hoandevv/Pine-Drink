package com.hoandev.pinedrink.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a validation error for a specific request field.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FieldError {
    private String field;
    private String message;
    private Object rejectedValue;
}
