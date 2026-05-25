package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "ce_branch", uniqueConstraints = @UniqueConstraint(columnNames = {"brand_id", "code"}))
public class Branch extends BaseEntity {

    private String code;

    private String name;

    private String address;

    private String phone;

    private String email;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    private String timezone = "Asia/Ho_Chi_Minh";

    @Column(name = "supports_pickup")
    private boolean supportsPickup = true;

    @Column(name = "supports_delivery")
    private boolean supportsDelivery = false;

    @Column(name = "average_preparation_minutes")
    private int averagePreparationMinutes = 15;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;
}
