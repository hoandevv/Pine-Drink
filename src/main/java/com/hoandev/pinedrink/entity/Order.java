package com.hoandev.pinedrink.entity;

import com.hoandev.pinedrink.repository.result.InvoiceHeaderResult;
import com.hoandev.pinedrink.repository.result.DailyRevenuePaymentResult;
import com.hoandev.pinedrink.repository.result.DailyRevenueSummaryResult;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@SqlResultSetMappings({
        @SqlResultSetMapping(name = "InvoiceHeaderResultMapping", classes = @ConstructorResult(targetClass = InvoiceHeaderResult.class, columns = {
                @ColumnResult(name = "orderId", type = String.class),
                @ColumnResult(name = "orderCode", type = String.class),
                @ColumnResult(name = "customerName", type = String.class),
                @ColumnResult(name = "subtotalAmount", type = BigDecimal.class),
                @ColumnResult(name = "discountAmount", type = BigDecimal.class),
                @ColumnResult(name = "totalAmount", type = BigDecimal.class),
                @ColumnResult(name = "orderTime", type = LocalDateTime.class),
                @ColumnResult(name = "branchName", type = String.class),
                @ColumnResult(name = "branchAddress", type = String.class)
        })),
        @SqlResultSetMapping(name = "DailyRevenueSummaryResultMapping", classes = @ConstructorResult(targetClass = DailyRevenueSummaryResult.class, columns = {
                @ColumnResult(name = "branchName", type = String.class),
                @ColumnResult(name = "branchAddress", type = String.class),
                @ColumnResult(name = "totalOrders", type = Long.class),
                @ColumnResult(name = "grossRevenue", type = BigDecimal.class),
                @ColumnResult(name = "totalDiscount", type = BigDecimal.class),
                @ColumnResult(name = "netRevenue", type = BigDecimal.class)
        })),
        @SqlResultSetMapping(name = "DailyRevenuePaymentResultMapping", classes = @ConstructorResult(targetClass = DailyRevenuePaymentResult.class, columns = {
                @ColumnResult(name = "paymentMethod", type = String.class),
                @ColumnResult(name = "orderCount", type = Long.class),
                @ColumnResult(name = "grossAmount", type = BigDecimal.class),
                @ColumnResult(name = "discountAmount", type = BigDecimal.class),
                @ColumnResult(name = "netAmount", type = BigDecimal.class)
        }))
})
@Table(name = "od_order")
public class Order extends BaseEntity {

    @Column(name = "order_code", nullable = false, unique = true)
    private String orderCode;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "order_type", nullable = false)
    private String orderType = "PICKUP";

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "UNPAID";

    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "delivery_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "pickup_time")
    private LocalDateTime pickupTime;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    private String note;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "prepared_at")
    private LocalDateTime preparedAt;

    @Column(name = "ready_at")
    private LocalDateTime readyAt;

    @Column(name = "delivering_at")
    private LocalDateTime deliveringAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerProfile customer;

    @PrePersist
    @Override
    protected void prePersist() {
        super.prePersist();
        if (getStatus() == null || "ACTIVE".equals(getStatus())) {
            setStatus("PENDING");
        }
    }
}
