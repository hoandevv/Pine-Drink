package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "cu_loyalty_account", uniqueConstraints = @UniqueConstraint(columnNames = "customer_id"))
@AttributeOverrides({
    @AttributeOverride(name = "createdBy", column = @Column(name = "created_by", insertable = false, updatable = false)),
    @AttributeOverride(name = "updatedBy", column = @Column(name = "updated_by", insertable = false, updatable = false))
})
public class LoyaltyAccount extends BaseEntity {

    @Column(nullable = false)
    private String tier = "SILVER";

    @Column(name = "points_balance", nullable = false)
    private int pointsBalance = 0;

    @Column(name = "lifetime_points", nullable = false)
    private int lifetimePoints = 0;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private CustomerProfile customer;

    @OneToMany(mappedBy = "loyaltyAccount")
    private List<LoyaltyPointHistory> loyaltyPointHistories;
}
