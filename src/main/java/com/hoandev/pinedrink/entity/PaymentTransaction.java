package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "py_transaction")
public class PaymentTransaction extends BaseEntity {

    @Column(name = "transaction_code", unique = true, nullable = false)
    private String transactionCode;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency = "VND";

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "failed_reason")
    private String failedReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_intent_id")
    private PaymentIntent paymentIntent;
}
