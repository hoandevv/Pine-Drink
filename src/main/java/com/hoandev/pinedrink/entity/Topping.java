package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "pr_topping", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class Topping extends BaseEntity {

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "group_name")
    private String groupName;


    @OneToMany(mappedBy = "topping")
    private List<ProductTopping> productToppings;

    @OneToMany(mappedBy = "topping")
    private List<BranchToppingAvailability> branchToppingAvailabilities;

    @OneToMany(mappedBy = "topping")
    private List<CartItemTopping> cartItemToppings;
}
