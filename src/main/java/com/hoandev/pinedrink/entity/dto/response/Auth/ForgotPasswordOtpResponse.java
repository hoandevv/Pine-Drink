package com.hoandev.pinedrink.entity.dto.response.Auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for forgot password OTP verification.
 * Returns a reset token that can be used to reset the password.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordOtpResponse {

    private String resetToken;
    private String tokenType;
    private long expiresIn;

}
