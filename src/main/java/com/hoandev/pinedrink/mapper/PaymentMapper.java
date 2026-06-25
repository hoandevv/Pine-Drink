package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.Order;
import com.hoandev.pinedrink.entity.PaymentTransaction;
import com.hoandev.pinedrink.entity.dto.response.Payment.PaymentTransactionResponse;
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

    private String resolveOrderPaymentStatus(Order order, String defaultOrderPaymentStatus) {
        if (order == null || order.getPaymentStatus() == null) {
            return defaultOrderPaymentStatus;
        }
        return order.getPaymentStatus();
    }
}
