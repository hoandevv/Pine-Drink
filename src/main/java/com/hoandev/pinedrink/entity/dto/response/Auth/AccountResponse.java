package com.hoandev.pinedrink.entity.dto.response.Auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    private String id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String status;
    private LocalDateTime lastLoginAt;
    private LocalDate dateOfBirth;
    private String gender;
    private ScopeAccessResponse scope;

    /**
     * Branch access summary for UI filtering.
     * SYSTEM means all branches; BRANCH means only listed branch IDs.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScopeAccessResponse {
        private String type;
        private List<String> branchIds;
    }
}
