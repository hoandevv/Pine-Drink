package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "pr_topping", uniqueConstraints = @UniqueConstraint(columnNames = {"brand_id", "code"}))
public class Topping extends BaseEntity {

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url")
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @OneToMany(mappedBy = "topping")
    private List<ProductTopping> productToppings;

    @OneToMany(mappedBy = "topping")
    private List<BranchToppingAvailability> branchToppingAvailabilities;

    @OneToMany(mappedBy = "topping")
    private List<CartItemTopping> cartItemToppings;
}
