package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "cu_loyalty_account", uniqueConstraints = @UniqueConstraint(columnNames = "customer_id"))
public class LoyaltyAccount extends BaseEntity {

    @Column(name = "tier", nullable = false)
    private String tier = "SILVER";

    @Column(name = "points_balance", nullable = false)
    private int pointsBalance = 0;

    @Column(name = "lifetime_points", nullable = false)
    private int lifetimePoints = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", unique = true, nullable = false)
    private CustomerProfile customer;

    @OneToMany(mappedBy = "loyaltyAccount")
    private List<LoyaltyPointHistory> loyaltyPointHistories;
}
