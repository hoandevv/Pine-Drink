package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "ce_brand")
public class Brand extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "tax_code")
    private String taxCode;

    private String address;

    private String phone;

    private String email;

    @Column(nullable = false)
    private String timezone = "Asia/Ho_Chi_Minh";

    @OneToMany(mappedBy = "brand")
    private List<BrandDomain> brandDomains;

    @OneToMany(mappedBy = "brand")
    private List<Branch> branches;

    @OneToMany(mappedBy = "brand")
    private List<Setting> settings;
}
