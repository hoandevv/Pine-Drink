package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "ce_branch_variant_daily_stock", uniqueConstraints = @UniqueConstraint(columnNames = {"branch_id", "variant_id", "stock_date"}))
public class BranchVariantDailyStock extends BaseEntity {

    @Column(name = "stock_date", nullable = false)
    private LocalDate stockDate;

    @Column(name = "daily_quantity", nullable = false)
    private int dailyQuantity = 0;

    @Column(name = "sold_quantity", nullable = false)
    private int soldQuantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;
}
