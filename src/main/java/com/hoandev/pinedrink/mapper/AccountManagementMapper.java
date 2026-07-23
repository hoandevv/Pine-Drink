package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.Account;
import com.hoandev.pinedrink.entity.AccountRoleAssignment;
import com.hoandev.pinedrink.entity.CustomerProfile;
import com.hoandev.pinedrink.entity.dto.response.Account.AccountDetailResponse;
import com.hoandev.pinedrink.entity.dto.response.Account.AccountListItemResponse;
import com.hoandev.pinedrink.entity.dto.response.Account.AccountRoleAssignmentResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccountManagementMapper {

    public AccountListItemResponse toListItem(Account account, List<String> roles) {
        return AccountListItemResponse.builder()
                .id(account.getId())
                .username(account.getUsername())
                .fullName(account.getFullName())
                .email(account.getEmail())
                .phone(account.getPhone())
                .avatarUrl(account.getAvatarUrl())
                .status(account.getStatus())
                .lastLoginAt(account.getLastLoginAt())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .roles(roles)
                .build();
    }

    public AccountDetailResponse toDetail(Account account,
                                          CustomerProfile customerProfile,
                                          List<AccountRoleAssignmentResponse> roleAssignments) {
        return AccountDetailResponse.builder()
                .id(account.getId())
                .username(account.getUsername())
                .fullName(account.getFullName())
                .email(account.getEmail())
                .phone(account.getPhone())
                .avatarUrl(account.getAvatarUrl())
                .status(account.getStatus())
                .lastLoginAt(account.getLastLoginAt())
                .dateOfBirth(customerProfile != null ? customerProfile.getDateOfBirth() : null)
                .gender(customerProfile != null ? customerProfile.getGender() : null)
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .roleAssignments(roleAssignments)
                .build();
    }

    public AccountRoleAssignmentResponse toRoleAssignmentResponse(AccountRoleAssignment assignment) {
        return AccountRoleAssignmentResponse.builder()
                .assignmentId(assignment.getId())
                .roleId(assignment.getRole().getId())
                .roleCode(assignment.getRole().getCode())
                .roleName(assignment.getRole().getName())
                .scopeId(assignment.getScope().getId())
                .scopeType(assignment.getScope().getScopeType())
                .scopeBranchId(assignment.getScope().getBranch() != null ? assignment.getScope().getBranch().getId() : null)
                .status(assignment.getStatus())
                .assignedAt(assignment.getAssignedAt())
                .expiresAt(assignment.getExpiresAt())
                .build();
    }
}
