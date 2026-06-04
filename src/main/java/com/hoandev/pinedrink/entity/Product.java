package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "pr_product", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "preparation_minutes", nullable = false)
    private int preparationMinutes = 10;

    @Column(name = "is_featured", nullable = false)
    private boolean isFeatured = false;

    @Column(name = "is_best_seller", nullable = false)
    private boolean isBestSeller = false;

    @Column(name = "available_ice_levels", nullable = false)
    private String availableIceLevels = "0,30,50,70,100";

    @Column(name = "available_sugar_levels", nullable = false)
    private String availableSugarLevels = "0,30,50,70,100";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "product")
    private List<ProductVariant> productVariants;

    @OneToMany(mappedBy = "product")
    private List<ProductTopping> productToppings;

    @OneToMany(mappedBy = "product")
    private List<BranchProductAvailability> branchProductAvailabilities;

    @OneToMany(mappedBy = "product")
    private List<CartItem> cartItems;
}
