package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "ca_cart")
@AttributeOverride(name = "updatedBy", column = @Column(name = "updated_by", insertable = false, updatable = false))
public class Cart extends BaseEntity {

    @Column(name = "session_id")
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerProfile customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @OneToMany(mappedBy = "cart")
    private List<CartItem> cartItems;
}
