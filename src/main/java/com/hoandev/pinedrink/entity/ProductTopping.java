package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pr_product_topping", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "topping_id"}))
public class ProductTopping extends BaseEntity {

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "max_quantity", nullable = false)
    private int maxQuantity = 3;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topping_id", nullable = false)
    private Topping topping;
}
