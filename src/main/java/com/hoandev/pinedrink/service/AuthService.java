package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Auth.LoginRequest;
import com.hoandev.pinedrink.entity.dto.request.Auth.RefreshTokenRequest;
import com.hoandev.pinedrink.entity.dto.request.Auth.RegisterRequest;
import com.hoandev.pinedrink.entity.dto.response.Auth.AccountResponse;
import com.hoandev.pinedrink.entity.dto.response.Auth.LoginResponse;
import com.hoandev.pinedrink.entity.dto.response.Auth.RefreshTokenResponse;
import com.hoandev.pinedrink.entity.dto.response.Auth.RegisterResponse;
import jakarta.servlet.http.HttpServletRequest;

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
}
