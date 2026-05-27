package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.Auth.LoginRequest;
import com.hoandev.pinedrink.entity.dto.request.Auth.RefreshTokenRequest;
import com.hoandev.pinedrink.entity.dto.request.Auth.RegisterRequest;
import com.hoandev.pinedrink.entity.dto.request.Auth.ResendRegisterOtpRequest;
import com.hoandev.pinedrink.entity.dto.request.Auth.VerifyRegisterOtpRequest;
import com.hoandev.pinedrink.entity.dto.request.Auth.ForgotPasswordRequest;
import com.hoandev.pinedrink.entity.dto.request.Auth.VerifyForgotPasswordOtpRequest;
import com.hoandev.pinedrink.entity.dto.request.Auth.ResetPasswordRequest;
import com.hoandev.pinedrink.entity.dto.response.Auth.AccountResponse;
import com.hoandev.pinedrink.entity.dto.response.Auth.LoginResponse;
import com.hoandev.pinedrink.entity.dto.response.Auth.RefreshTokenResponse;
import com.hoandev.pinedrink.entity.dto.response.Auth.RegisterResponse;
import com.hoandev.pinedrink.entity.dto.response.Auth.ForgotPasswordOtpResponse;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing authentication and registration endpoints.
 * <p>
 * All responses are wrapped in {@link BaseResponse} with HTTP 200 (OK) on success,
 * except {@link #register(RegisterRequest, HttpServletRequest)} which returns 201 (Created).
 * Error handling is delegated to {@link com.hoandev.pinedrink.exception.GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new account and sends an OTP verification email.
     * <p>
     * The account is created in an {@code INACTIVE} state. An OTP is stored in Redis
     * and a {@link com.hoandev.pinedrink.queue.event.email.RegisterOtpEmailEvent} is
     * published via RabbitMQ for asynchronous email delivery.
     *
     * @param request     registration payload (username, password, email, fullName, phone)
     * @param httpRequest the incoming HTTP request (used for client context if needed)
     * @return {@code 201 Created} with a {@link RegisterResponse} containing account summary
     */
    @PostMapping("/register")
    public ResponseEntity<BaseResponse<RegisterResponse>> register(
            @RequestBody @Valid RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        log.debug("Register request received");
        RegisterResponse response = authService.register(request, httpRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Account created successfully"));
    }

    /**
     * Verifies the OTP for account activation.
     * <p>
     * On success the account status transitions to {@code ACTIVE}, the default
     * customer role is assigned, and a customer profile is created.
     *
     * @param request email and OTP to verify
     * @return {@code 200 OK} with a success message
     */
    @PostMapping("/register/verify-otp")
    public ResponseEntity<BaseResponse<Void>> verifyRegistrationOtp(
            @RequestBody @Valid VerifyRegisterOtpRequest request
    ) {
        authService.verifyRegistrationOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(
                BaseResponse.success(null, "Account activated successfully")
        );
    }

    /**
     * Resends the registration OTP after a cooldown period.
     * <p>
     * A new OTP is generated and the previous one is invalidated.
     * Subsequent resends are rate-limited by a 60-second cooldown stored in Redis.
     *
     * @param request email to resend the OTP to
     * @return {@code 200 OK} with a success message
     */
    @PostMapping("/register/resend-otp")
    public ResponseEntity<BaseResponse<Void>> resendRegistrationOtp(
            @RequestBody @Valid ResendRegisterOtpRequest request
    ) {
        authService.resendRegistrationOtp(request.getEmail());
        return ResponseEntity.ok(
                BaseResponse.success(null, "OTP resent successfully")
        );
    }

    /**
     * Authenticates an account and issues JWT tokens.
     * <p>
     * Accepts username or email as the login identifier.
     * Returns an access token (short-lived) and a refresh token (long-lived, SHA-256 hashed).
     *
     * @param request login credentials (username/email + password)
     * @return {@code 200 OK} with {@link LoginResponse} containing tokens and account info
     */
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest request
    ) {
        log.debug("Login request received");
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(
                BaseResponse.success(response, "Login successfully")
        );
    }

    /**
     * Rotates an expired or expiring refresh token.
     * <p>
     * The old refresh token is revoked and a new pair (access + refresh) is issued.
     *
     * @param request the current refresh token
     * @return {@code 200 OK} with {@link RefreshTokenResponse} containing new tokens
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<BaseResponse<RefreshTokenResponse>> refresh(
            @RequestBody @Valid RefreshTokenRequest request
    ) {
        log.debug("Refresh token request received");
        RefreshTokenResponse response = authService.refresh(request);
        return ResponseEntity.ok(
                BaseResponse.success(response, "Token refreshed successfully")
        );
    }

    /**
     * Revokes the given refresh token, ending the session.
     *
     * @param request the refresh token to revoke
     * @return {@code 200 OK} with a success message
     */
    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(
            @RequestBody @Valid RefreshTokenRequest request
    ) {
        authService.logout(request);
        return ResponseEntity.ok(
                BaseResponse.success(null, "Logout successfully")
        );
    }

    /**
     * Retrieves the profile of the currently authenticated account.
     * <p>
     * Requires a valid Bearer token in the {@code Authorization} header.
     *
     * @return {@code 200 OK} with {@link AccountResponse} containing profile details
     */
    @GetMapping("/me")
    public ResponseEntity<BaseResponse<AccountResponse>> getCurrentProfile() {
        AccountResponse response = authService.getCurrentProfile();
        return ResponseEntity.ok(
                BaseResponse.success(response)
        );
    }

    /**
     * Initiates the password reset process by sending an OTP via email.
     * <p>
     * Generates an OTP, stores it in Redis with expiration time,
     * and sends an email containing the OTP to the user.
     *
     * @param request containing the email address
     * @return {@code 200 OK} with a success message
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<BaseResponse<Void>> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest request
    ) {
        log.debug("Forgot password request received for email: {}", request.getEmail());
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(
                BaseResponse.success(null, "Password reset OTP has been sent to your email")
        );
    }

    /**
     * Verifies the forgot password OTP and issues a reset token.
     * <p>
     * Validates the OTP, and if correct, returns a JWT reset token
     * that can be used to call the reset password endpoint.
     *
     * @param request containing email and OTP
     * @return {@code 200 OK} with {@link ForgotPasswordOtpResponse} containing reset token
     */
    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<BaseResponse<ForgotPasswordOtpResponse>> verifyForgotPasswordOtp(
            @RequestBody @Valid VerifyForgotPasswordOtpRequest request
    ) {
        log.debug("Verify forgot password OTP request received");
        ForgotPasswordOtpResponse response = authService.verifyForgotPasswordOtp(
                request.getEmail(),
                request.getOtp()
        );
        return ResponseEntity.ok(
                BaseResponse.success(response, "OTP verified successfully")
        );
    }

    /**
     * Resets the account password for the authenticated user.
     * <p>
     * Requires authentication via reset token (Bearer token in Authorization header).
     * Validates that new password matches confirm password, then updates the password.
     *
     * @param request containing newPassword and confirmPassword
     * @return {@code 200 OK} with success message
     */
    @PostMapping("/reset-password")
    public ResponseEntity<BaseResponse<Void>> resetPassword(
            @RequestBody @Valid ResetPasswordRequest request
    ) {
        log.debug("Reset password request received");
        authService.resetPassword(
                request.getNewPassword(),
                request.getConfirmPassword()
        );
        return ResponseEntity.ok(
                BaseResponse.success(null, "Password has been reset successfully")
        );
    }
}