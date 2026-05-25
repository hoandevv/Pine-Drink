package com.hoandev.pinedrink.entity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BrandRequest {
    @NotBlank
    private String code;

    @NotBlank
    private String name;

    private String legalName;
    private String taxCode;
    private String address;
    private String phone;

    @Email
    private String email;

    private String timezone;
}
