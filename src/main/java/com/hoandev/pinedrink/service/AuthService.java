package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Auth.LoginRequest;
import com.hoandev.pinedrink.entity.dto.request.Auth.RefreshTokenRequest;
import com.hoandev.pinedrink.entity.dto.request.Auth.RegisterRequest;
import com.hoandev.pinedrink.entity.dto.response.Auth.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * Service interface for authentication and registration operations.
 */
public interface AuthService {

    /**
     * Authenticates an account using username/email and password,
     * then issues access and refresh tokens.
     *
     * @param request login credentials containing username/email and password
     * @return {@link LoginResponse} containing access token, refresh token, and account info
     */
    LoginResponse login(LoginRequest request);

    /**
     * Validates a refresh token and issues a new access token pair.
     *
     * @param request containing the refresh token to validate
     * @return {@link RefreshTokenResponse} containing new access and refresh tokens
     */
    RefreshTokenResponse refresh(RefreshTokenRequest request);

    /**
     * Revokes the given refresh token, effectively logging out the session.
     *
     * @param request containing the refresh token to revoke
     */
    void logout(RefreshTokenRequest request);

    /**
     * Retrieves the currently authenticated account's profile information.
     *
     * @return {@link AccountResponse} containing the current account's details
     */
    AccountResponse getCurrentProfile();

    /**
     * Retrieves current authenticated account permissions for frontend access control.
     *
     * @return raw permission codes without PERM_ prefix
     */
    List<String> getCurrentPermissions();

    /**
     * Creates a new inactive account, generates a registration OTP,
     * stores it in Redis, and publishes an email event to deliver the OTP.
     *
     * @param request     registration details (username, password, email, etc.)
     * @param httpRequest the incoming HTTP request (for client info if needed)
     * @return {@link RegisterResponse} confirming the account creation
     */
    RegisterResponse register(RegisterRequest request, HttpServletRequest httpRequest);

    /**
     * Verifies the registration OTP and activates the account.
     * On success, assigns the default customer role and creates a customer profile.
     *
     * @param email the email address associated with the registration
     * @param otp   the one-time password to verify
     */
    void verifyRegistrationOtp(String email, String otp);

    /**
     * Generates a fresh registration OTP and publishes it to the email queue.
     * Enforces a cooldown period between resend requests.
     *
     * @param email the email address to resend the OTP to
     */
    void resendRegistrationOtp(String email);

    /**
     * Initiates the password reset process by generating an OTP
     * and sending it via email.
     *
     * @param email the email address of the account requesting password reset
     */
    void forgotPassword(String email);

    /**
     * Verifies the forgot password OTP and issues a reset token.
     * The reset token can be used to call the reset password endpoint.
     *
     * @param email the email address
     * @param otp the OTP to verify
     * @return reset token information (token, tokenType, expiresIn)
     */
    ForgotPasswordOtpResponse verifyForgotPasswordOtp(String email, String otp);

    /**
     * Resets the account password for the authenticated user.
     * Requires authentication via reset token (Bearer token).
     *
     * @param newPassword the new password to set
     * @param confirmPassword confirmation of the new password
     */
    void resetPassword(String newPassword, String confirmPassword);
}
