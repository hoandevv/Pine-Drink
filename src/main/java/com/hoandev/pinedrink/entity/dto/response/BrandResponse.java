package com.hoandev.pinedrink.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BrandResponse {
    private String id;
    private String code;
    private String name;
    private String legalName;
    private String taxCode;
    private String address;
    private String phone;
    private String email;
    private String timezone;
    private String status;
    private LocalDateTime createdAt;
}
