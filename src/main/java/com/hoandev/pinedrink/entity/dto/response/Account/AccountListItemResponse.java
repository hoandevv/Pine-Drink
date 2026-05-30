package com.hoandev.pinedrink.entity.dto.response.Account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountListItemResponse {
    private String id;
    private String brandId;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> roles;
}
