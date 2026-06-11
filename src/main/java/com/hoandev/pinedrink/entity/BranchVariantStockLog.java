package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ce_branch_variant_stock_log")
public class BranchVariantStockLog extends BaseEntity {

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "before_daily_quantity", nullable = false)
    private int beforeDailyQuantity;

    @Column(name = "after_daily_quantity", nullable = false)
    private int afterDailyQuantity;

    @Column(name = "before_sold_quantity", nullable = false)
    private int beforeSoldQuantity;

    @Column(name = "after_sold_quantity", nullable = false)
    private int afterSoldQuantity;

    @Column(name = "before_reserved_quantity", nullable = false)
    private int beforeReservedQuantity;

    @Column(name = "after_reserved_quantity", nullable = false)
    private int afterReservedQuantity;

    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_stock_id", nullable = false)
    private BranchVariantDailyStock dailyStock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
}
