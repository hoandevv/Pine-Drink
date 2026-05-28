package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "py_refund")
@AttributeOverrides({
    @AttributeOverride(name = "createdAt", column = @Column(name = "requested_at", nullable = false, updatable = false)),
    @AttributeOverride(name = "createdBy", column = @Column(name = "created_by", insertable = false, updatable = false)),
    @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", insertable = false, updatable = false)),
    @AttributeOverride(name = "updatedBy", column = @Column(name = "updated_by", insertable = false, updatable = false))
})
public class Refund extends BaseEntity {

    @Column(name = "refund_code", nullable = false, unique = true)
    private String refundCode;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    private String reason;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private PaymentTransaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private Account requestedBy;
}
