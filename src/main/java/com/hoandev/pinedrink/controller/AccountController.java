package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.Account.AdminResetPasswordRequest;
import com.hoandev.pinedrink.entity.dto.request.Account.AssignRoleRequest;
import com.hoandev.pinedrink.entity.dto.request.Account.CreateAccountRequest;
import com.hoandev.pinedrink.entity.dto.request.Account.UpdateAccountRequest;
import com.hoandev.pinedrink.entity.dto.request.Account.UpdateAccountStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.Account.AccountDetailResponse;
import com.hoandev.pinedrink.entity.dto.response.Account.AccountListItemResponse;
import com.hoandev.pinedrink.entity.dto.response.Account.AccountRoleAssignmentResponse;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_ACCOUNT_VIEW')")
    public ResponseEntity<BaseResponse<PageResponse<AccountListItemResponse>>> searchAccounts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String roleCode,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Searching accounts with keyword={}, status={}, roleCode={}", keyword, status, roleCode);
        PageResponse<AccountListItemResponse> response = accountService.searchAccounts(keyword, status, roleCode, pageable);
        return ResponseEntity.ok(BaseResponse.success(response, "Accounts retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_ACCOUNT_VIEW')")
    public ResponseEntity<BaseResponse<AccountDetailResponse>> getAccountDetail(@PathVariable String id) {
        log.info("Getting account detail: id={}", id);
        return ResponseEntity.ok(BaseResponse.success(accountService.getAccountDetail(id), "Account retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_ACCOUNT_CREATE')")
    public ResponseEntity<BaseResponse<AccountDetailResponse>> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        log.info("Creating account: username={}", request.getUsername());
        AccountDetailResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Account created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_ACCOUNT_UPDATE')")
    public ResponseEntity<BaseResponse<AccountDetailResponse>> updateAccount(
            @PathVariable String id,
            @Valid @RequestBody UpdateAccountRequest request) {
        log.info("Updating account: id={}", id);
        return ResponseEntity.ok(BaseResponse.success(accountService.updateAccount(id, request), "Account updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERM_ACCOUNT_CHANGE_STATUS')")
    public ResponseEntity<BaseResponse<AccountDetailResponse>> updateAccountStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateAccountStatusRequest request) {
        log.info("Updating account status: id={}, status={}", id, request.getStatus());
        return ResponseEntity.ok(BaseResponse.success(accountService.updateAccountStatus(id, request), "Account status updated successfully"));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('PERM_ACCOUNT_RESET_PASSWORD')")
    public ResponseEntity<BaseResponse<Void>> adminResetPassword(
            @PathVariable String id,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        log.info("Admin resetting password for account: id={}", id);
        accountService.adminResetPassword(id, request);
        return ResponseEntity.ok(BaseResponse.success(null, "Password reset successfully"));
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('PERM_ACCOUNT_ROLE_VIEW')")
    public ResponseEntity<BaseResponse<List<AccountRoleAssignmentResponse>>> getAccountRoles(@PathVariable String id) {
        log.info("Getting account roles: id={}", id);
        return ResponseEntity.ok(BaseResponse.success(accountService.getAccountRoles(id), "Account roles retrieved successfully"));
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('PERM_ACCOUNT_ROLE_ASSIGN')")
    public ResponseEntity<BaseResponse<List<AccountRoleAssignmentResponse>>> assignRole(
            @PathVariable String id,
            @Valid @RequestBody AssignRoleRequest request) {
        log.info("Assigning role to account: id={}, roleCode={}", id, request.getRoleCode());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(accountService.assignRole(id, request), "Role assigned successfully"));
    }

    @DeleteMapping("/{id}/roles/{assignmentId}")
    @PreAuthorize("hasAuthority('PERM_ACCOUNT_ROLE_REVOKE')")
    public ResponseEntity<BaseResponse<Void>> revokeRole(@PathVariable String id, @PathVariable String assignmentId) {
        log.info("Revoking role assignment: accountId={}, assignmentId={}", id, assignmentId);
        accountService.revokeRole(id, assignmentId);
        return ResponseEntity.ok(BaseResponse.success(null, "Role assignment revoked successfully"));
    }
}
