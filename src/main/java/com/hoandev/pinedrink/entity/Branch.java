package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "ce_branch", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class Branch extends BaseEntity {

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String address;

    private String phone;

    private String email;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(nullable = false)
    private String timezone = "Asia/Ho_Chi_Minh";

    @Column(name = "supports_pickup", nullable = false)
    private boolean supportsPickup = true;

    @Column(name = "supports_delivery", nullable = false)
    private boolean supportsDelivery = false;

    @Column(name = "average_preparation_minutes", nullable = false)
    private int averagePreparationMinutes = 15;

}
