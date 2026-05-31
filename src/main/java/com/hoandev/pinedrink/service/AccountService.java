package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Account.AdminResetPasswordRequest;
import com.hoandev.pinedrink.entity.dto.request.Account.AssignRoleRequest;
import com.hoandev.pinedrink.entity.dto.request.Account.CreateAccountRequest;
import com.hoandev.pinedrink.entity.dto.request.Account.UpdateAccountRequest;
import com.hoandev.pinedrink.entity.dto.request.Account.UpdateAccountStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.Account.AccountDetailResponse;
import com.hoandev.pinedrink.entity.dto.response.Account.AccountListItemResponse;
import com.hoandev.pinedrink.entity.dto.response.Account.AccountRoleAssignmentResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AccountService {

    PageResponse<AccountListItemResponse> searchAccounts(String keyword,
                                                         String status,
                                                         String roleCode,
                                                         String brandId,
                                                         Pageable pageable);

    AccountDetailResponse getAccountDetail(String id);

    AccountDetailResponse createAccount(CreateAccountRequest request);

    AccountDetailResponse updateAccount(String id, UpdateAccountRequest request);

    AccountDetailResponse updateAccountStatus(String id, UpdateAccountStatusRequest request);

    void adminResetPassword(String id, AdminResetPasswordRequest request);

    List<AccountRoleAssignmentResponse> getAccountRoles(String id);

    List<AccountRoleAssignmentResponse> assignRole(String id, AssignRoleRequest request);

    void revokeRole(String id, String assignmentId);
}
