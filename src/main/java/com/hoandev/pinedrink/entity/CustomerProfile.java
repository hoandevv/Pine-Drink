package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "cu_customer_profile", uniqueConstraints = @UniqueConstraint(columnNames = "account_id"))
public class CustomerProfile extends BaseEntity {

    @Column(name = "customer_code", nullable = false, unique = true)
    private String customerCode;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;

    private String email;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String gender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @OneToMany(mappedBy = "customer")
    private List<CustomerAddress> customerAddresses;

    @OneToOne(mappedBy = "customer")
    private LoyaltyAccount loyaltyAccount;

    @OneToMany(mappedBy = "customer")
    private List<Cart> carts;
}
