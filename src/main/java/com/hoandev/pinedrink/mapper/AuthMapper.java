package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.Account;
import com.hoandev.pinedrink.entity.AccountRoleAssignment;
import com.hoandev.pinedrink.entity.dto.response.Auth.AccountResponse;
import com.hoandev.pinedrink.entity.dto.response.Auth.LoginResponse;
import com.hoandev.pinedrink.entity.dto.response.Auth.RegisterResponse;
import com.hoandev.pinedrink.repository.AccountRoleAssignmentRepository;
import com.hoandev.pinedrink.security.JwtTokenProvider;
import com.hoandev.pinedrink.utils.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper for Auth-related entities and DTOs.
 * Handles conversion between Account entities and response DTOs.
 */
@Component
@RequiredArgsConstructor
public class AuthMapper {

    private final JwtTokenProvider jwtTokenProvider;
    private final AccountRoleAssignmentRepository assignmentRepository;

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
        response.setUsername(account.getUsername());
        response.setFullName(account.getFullName());
        response.setEmail(account.getEmail());
        response.setPhone(account.getPhone());
        response.setAvatarUrl(account.getAvatarUrl());
        response.setStatus(account.getStatus());
        response.setAuthProvider(account.getAuthProvider());
        response.setHasLocalPassword(Boolean.TRUE.equals(account.getHasLocalPassword()));
        response.setLastLoginAt(account.getLastLoginAt());
        response.setScope(buildScopeAccess(account.getId()));
        return response;
    }

    /**
     * Builds account branch access for FE-side filtering.
     * SYSTEM scope grants all branches; otherwise branch IDs are collected from active BRANCH scopes.
     *
     * @param accountId the account ID
     * @return scope access summary
     */
    public AccountResponse.ScopeAccessResponse buildScopeAccess(String accountId) {
        List<AccountRoleAssignment> assignments = assignmentRepository.findActiveAssignmentsByAccountId(
                accountId, LocalDateTime.now());

        boolean systemAccess = assignments.stream()
                .anyMatch(assignment -> Constants.SCOPE_SYSTEM.equals(assignment.getScope().getScopeType()));
        if (systemAccess) {
            return AccountResponse.ScopeAccessResponse.builder()
                    .type(Constants.SCOPE_SYSTEM)
                    .branchIds(List.of())
                    .build();
        }

        List<String> branchIds = assignments.stream()
                .map(AccountRoleAssignment::getScope)
                .filter(scope -> Constants.SCOPE_BRANCH.equals(scope.getScopeType()))
                .filter(scope -> scope.getBranch() != null)
                .map(scope -> scope.getBranch().getId())
                .distinct()
                .toList();

        return AccountResponse.ScopeAccessResponse.builder()
                .type(Constants.SCOPE_BRANCH)
                .branchIds(branchIds)
                .build();
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
