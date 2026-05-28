package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "iv_stock", uniqueConstraints = @UniqueConstraint(columnNames = {"branch_id", "ingredient_id"}))
@AttributeOverrides({
    @AttributeOverride(name = "createdAt", column = @Column(name = "created_at", insertable = false, updatable = false)),
    @AttributeOverride(name = "createdBy", column = @Column(name = "created_by", insertable = false, updatable = false)),
    @AttributeOverride(name = "updatedBy", column = @Column(name = "updated_by", insertable = false, updatable = false))
})
public class Stock extends BaseEntity {

    @Column(name = "quantity_on_hand", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    @Column(name = "reserved_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal reservedQuantity = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;
}
