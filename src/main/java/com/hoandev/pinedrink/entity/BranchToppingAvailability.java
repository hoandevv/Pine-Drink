package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "mn_branch_topping_availability", uniqueConstraints = @UniqueConstraint(columnNames = {"branch_id", "topping_id"}))
@AttributeOverrides({
    @AttributeOverride(name = "createdAt", column = @Column(name = "created_at", insertable = false, updatable = false)),
    @AttributeOverride(name = "createdBy", column = @Column(name = "created_by", insertable = false, updatable = false))
})
public class BranchToppingAvailability extends BaseEntity {

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable = true;

    @Column(name = "sold_out_reason")
    private String soldOutReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topping_id", nullable = false)
    private Topping topping;
}
