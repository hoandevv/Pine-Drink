package com.hoandev.pinedrink.entity.dto.response.Account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRoleAssignmentResponse {
    private String assignmentId;
    private String roleId;
    private String roleCode;
    private String roleName;
    private String scopeId;
    private String scopeType;
    private String scopeBranchId;
    private String status;
    private LocalDateTime assignedAt;
    private LocalDateTime expiresAt;
}
