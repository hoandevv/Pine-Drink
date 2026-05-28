package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.Account;
import com.hoandev.pinedrink.entity.dto.response.Auth.AccountResponse;
import com.hoandev.pinedrink.entity.dto.response.Auth.LoginResponse;
import com.hoandev.pinedrink.entity.dto.response.Auth.RegisterResponse;
import com.hoandev.pinedrink.security.JwtTokenProvider;
import com.hoandev.pinedrink.utils.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper for Auth-related entities and DTOs.
 * Handles conversion between Account entities and response DTOs.
 */
@Component
@RequiredArgsConstructor
public class AuthMapper {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Maps an Account entity to AccountResponse DTO.
     *
     * @param account the account entity
     * @return the account response DTO
     */
    public AccountResponse toAccountResponse(Account account) {
        if (account == null) {
            return null;
        }

        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setBrandId(account.getBrand() != null ? account.getBrand().getId() : null);
        response.setUsername(account.getUsername());
        response.setFullName(account.getFullName());
        response.setEmail(account.getEmail());
        response.setPhone(account.getPhone());
        response.setAvatarUrl(account.getAvatarUrl());
        response.setStatus(account.getStatus());
        response.setLastLoginAt(account.getLastLoginAt());
        return response;
    }

    /**
     * Builds a RegisterResponse from an Account entity.
     *
     * @param account the registered account
     * @return the register response DTO
     */
    public RegisterResponse toRegisterResponse(Account account) {
        if (account == null) {
            return null;
        }

        return RegisterResponse.builder()
                .userId(account.getId())
                .username(account.getUsername())
                .email(account.getEmail())
                .message("Account created. Please verify your email with the OTP sent.")
                .build();
    }

    /**
     * Builds a LoginResponse from tokens and account.
     *
     * @param accessToken the access token
     * @param refreshToken the refresh token
     * @param account the account entity
     * @return the login response DTO
     */
    public LoginResponse toLoginResponse(String accessToken, String refreshToken, Account account) {
        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType(Constants.TOKEN_TYPE_BEARER);
        response.setExpiresIn(jwtTokenProvider.getAccessTokenExpiresInSeconds());
        response.setAccount(toAccountResponse(account));
        return response;
    }
}
