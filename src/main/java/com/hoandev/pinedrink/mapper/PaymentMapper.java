package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.Order;
import com.hoandev.pinedrink.entity.PaymentTransaction;
import com.hoandev.pinedrink.entity.Refund;
import com.hoandev.pinedrink.entity.dto.response.Payment.PaymentTransactionResponse;
import com.hoandev.pinedrink.entity.dto.response.Payment.RefundResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentTransactionResponse toResponse(PaymentTransaction transaction, String defaultOrderPaymentStatus) {
        if (transaction == null) {
            return null;
        }

        Order order = transaction.getOrder();

        return PaymentTransactionResponse.builder()
                .id(transaction.getId())
                .transactionCode(transaction.getTransactionCode())
                .orderId(order != null ? order.getId() : null)
                .orderCode(order != null ? order.getOrderCode() : null)
                .provider(transaction.getProvider())
                .paymentMethod(transaction.getPaymentMethod())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus())
                .orderPaymentStatus(resolveOrderPaymentStatus(order, defaultOrderPaymentStatus))
                .paidAt(transaction.getPaidAt())
                .failedReason(transaction.getFailedReason())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }

    public RefundResponse toRefundResponse(Refund refund) {
        if (refund == null) {
            return null;
        }

        PaymentTransaction transaction = refund.getTransaction();
        Order order = transaction != null ? transaction.getOrder() : null;

        RefundResponse response = new RefundResponse();
        response.setId(refund.getId());
        response.setRefundCode(refund.getRefundCode());
        response.setTransactionId(transaction != null ? transaction.getId() : null);
        response.setTransactionCode(transaction != null ? transaction.getTransactionCode() : null);
        response.setOrderId(order != null ? order.getId() : null);
        response.setOrderCode(order != null ? order.getOrderCode() : null);
        response.setAmount(refund.getAmount());
        response.setReason(refund.getReason());
        response.setStatus(refund.getStatus());
        response.setRequestedById(refund.getRequestedBy() != null ? refund.getRequestedBy().getId() : null);
        response.setRequestedByUsername(refund.getRequestedBy() != null ? refund.getRequestedBy().getUsername() : null);
        response.setRequestedAt(refund.getCreatedAt());
        response.setCompletedAt(refund.getCompletedAt());
        return response;
    }

    private String resolveOrderPaymentStatus(Order order, String defaultOrderPaymentStatus) {
        if (order == null || order.getPaymentStatus() == null) {
            return defaultOrderPaymentStatus;
        }
        return order.getPaymentStatus();
    }
}
