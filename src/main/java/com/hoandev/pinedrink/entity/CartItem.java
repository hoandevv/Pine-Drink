package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "ca_cart_item")
public class CartItem extends BaseEntity {

    @Column(nullable = false)
    private int quantity;

    @Column(name = "sugar_level", nullable = false)
    private String sugarLevel = "NORMAL";

    @Column(name = "ice_level", nullable = false)
    private String iceLevel = "NORMAL";

    private String note;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @OneToMany(mappedBy = "cartItem")
    private List<CartItemTopping> cartItemToppings;
}
