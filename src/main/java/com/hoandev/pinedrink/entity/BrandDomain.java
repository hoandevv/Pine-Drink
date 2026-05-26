package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ce_brand_domain", uniqueConstraints = @UniqueConstraint(columnNames = {"domain", "public_key"}))
public class BrandDomain extends BaseEntity {

    @Column(nullable = false)
    private String domain;

    @Column(name = "public_key", nullable = false, unique = true)
    private String publicKey;

    @Column(nullable = false)
    private String channel = "WEB";

    @Column(name = "allow_public_register", nullable = false)
    private boolean allowPublicRegister = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;
}
