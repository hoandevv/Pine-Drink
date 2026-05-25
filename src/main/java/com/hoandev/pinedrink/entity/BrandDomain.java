package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ce_brand_domain", uniqueConstraints = @UniqueConstraint(columnNames = {"domain", "public_key"}))
public class BrandDomain extends BaseEntity {

    private String domain;

    @Column(name = "public_key", unique = true)
    private String publicKey;

    private String channel = "WEB";

    @Column(name = "allow_public_register")
    private boolean allowPublicRegister = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;
}
