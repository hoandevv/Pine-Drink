package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "pr_product_variant", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "variant_code"}))
public class ProductVariant extends BaseEntity {

    @Column(name = "variant_code", nullable = false)
    private String variantCode;

    @Column(name = "variant_name", nullable = false)
    private String variantName;

    @Column(name = "size_label")
    private String sizeLabel;

    @Column(name = "price_delta", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceDelta = BigDecimal.ZERO;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @OneToMany(mappedBy = "variant")
    private List<CartItem> cartItems;
}
