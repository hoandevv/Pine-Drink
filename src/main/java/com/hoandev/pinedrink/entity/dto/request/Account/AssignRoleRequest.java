package com.hoandev.pinedrink.entity.dto.request.Account;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignRoleRequest {

    @NotBlank(message = "Role code is required")
    private String roleCode;

    @Builder.Default
    private String scopeType = "SYSTEM";

    private String brandId;

    private String branchId;

    private LocalDateTime expiresAt;
}
