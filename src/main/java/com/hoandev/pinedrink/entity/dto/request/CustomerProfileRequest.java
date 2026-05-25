package com.hoandev.pinedrink.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileRequest {
    @NotBlank
    private String accountId;

    @NotBlank
    private String fullName;

    private String phone;
    private String email;
    private LocalDate dateOfBirth;
    private String gender;
}
