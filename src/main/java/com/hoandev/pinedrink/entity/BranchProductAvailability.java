package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "mn_branch_product_availability", uniqueConstraints = @UniqueConstraint(columnNames = {"branch_id", "product_id"}))
@AttributeOverrides({
    @AttributeOverride(name = "createdAt", column = @Column(name = "created_at", insertable = false, updatable = false)),
    @AttributeOverride(name = "createdBy", column = @Column(name = "created_by", insertable = false, updatable = false))
})
public class BranchProductAvailability extends BaseEntity {

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable = true;

    @Column(name = "sale_price", precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "sold_out_reason")
    private String soldOutReason;

    @Column(name = "available_from")
    private LocalDateTime availableFrom;

    @Column(name = "available_to")
    private LocalDateTime availableTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}
