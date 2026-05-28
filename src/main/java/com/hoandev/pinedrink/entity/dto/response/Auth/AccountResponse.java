package com.hoandev.pinedrink.entity.dto.response.Auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    private String id;
    private String brandId;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String status;
    private LocalDateTime lastLoginAt;
}
