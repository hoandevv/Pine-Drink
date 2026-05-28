package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "iv_stock_movement")
@AttributeOverrides({
    @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", insertable = false, updatable = false)),
    @AttributeOverride(name = "updatedBy", column = @Column(name = "updated_by", insertable = false, updatable = false))
})
public class StockMovement extends BaseEntity {

    @Column(name = "order_id", columnDefinition = "CHAR(36)", insertable = false, updatable = false)
    private String orderId;

    @Column(name = "movement_type", nullable = false)
    private String movementType;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "before_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal beforeQuantity;

    @Column(name = "after_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal afterQuantity;

    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
}
