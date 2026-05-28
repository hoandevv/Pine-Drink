package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "py_payment_intent", uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "provider"}))
@AttributeOverrides({
    @AttributeOverride(name = "createdBy", column = @Column(name = "created_by", insertable = false, updatable = false)),
    @AttributeOverride(name = "updatedBy", column = @Column(name = "updated_by", insertable = false, updatable = false))
})
public class PaymentIntent extends BaseEntity {

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency = "VND";

    @Column(name = "request_payload", columnDefinition = "json")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "json")
    private String responsePayload;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
}
