package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.Order;
import com.hoandev.pinedrink.entity.PaymentIntent;
import com.hoandev.pinedrink.entity.PaymentTransaction;
import com.hoandev.pinedrink.entity.dto.request.Payment.RecordOfflinePaymentRequest;
import com.hoandev.pinedrink.entity.dto.response.Payment.PaymentTransactionResponse;
import com.hoandev.pinedrink.entity.enums.OrderStatus;
import com.hoandev.pinedrink.entity.enums.PaymentStatus;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.PaymentMapper;
import com.hoandev.pinedrink.repository.OrderRepository;
import com.hoandev.pinedrink.repository.PaymentIntentRepository;
import com.hoandev.pinedrink.repository.PaymentTransactionRepository;
import com.hoandev.pinedrink.service.AccessScopeService;
import com.hoandev.pinedrink.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final String METHOD_CASH = "CASH";
    private static final String METHOD_COD = "COD";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PAID = PaymentStatus.PAID.getValue();
    private static final String STATUS_UNPAID = PaymentStatus.UNPAID.getValue();
    private static final String ORDER_CANCELLED = OrderStatus.CANCELLED.getValue();
    private static final String ORDER_REJECTED = OrderStatus.REJECTED.getValue();

    private final OrderRepository orderRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentMapper paymentMapper;
    private final AccessScopeService accessScopeService;

    @Override
    @Transactional
    public PaymentTransactionResponse recordOfflinePayment(RecordOfflinePaymentRequest request) {
        String paymentMethod = normalizeOfflineMethod(request.getPaymentMethod());
        LocalDateTime now = LocalDateTime.now();
        Order order = orderRepository.findByIdForUpdate(request.getOrderId())
                .orElseThrow(() -> new BaseException(ErrorCode.ORDER_001));

        validateOrderCanRecordOfflinePayment(order, paymentMethod);

        if (STATUS_PAID.equals(order.getPaymentStatus())) {
            return handleAlreadyPaidOrder(order, paymentMethod);
        }

        PaymentIntent intent = getOrCreateIntent(order, paymentMethod);
        PaymentTransaction transaction = paymentTransactionRepository
                .findLatestByOrderAndMethodAndStatus(order.getId(), paymentMethod, STATUS_PENDING)
                .orElseGet(() -> createPendingTransaction(order, intent, paymentMethod));

        transaction = markOfflinePaymentAsPaid(order, intent, transaction, now);

        log.info("Offline payment recorded: orderId={}, transactionId={}, method={}",
                order.getId(), transaction.getId(), paymentMethod);
        return toResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentTransactionResponse getLatestOrderPaymentStatus(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BaseException(ErrorCode.ORDER_001));

        accessScopeService.assertCanViewOrder(order);

        return findLatestTransaction(orderId)
                .map(this::toResponse)
                .orElseThrow(() -> new BaseException(ErrorCode.COM_005));
    }

    private PaymentTransactionResponse handleAlreadyPaidOrder(Order order, String paymentMethod) {
        return findLatestTransaction(order.getId())
                .map(this::toResponse)
                .orElseGet(() -> {
                    PaymentIntent intent = getOrCreateIntent(order, paymentMethod);
                    PaymentTransaction transaction = createPaidTransactionForAlreadyPaidOrder(order, intent, paymentMethod, LocalDateTime.now());
                    return toResponse(transaction);
                });
    }

    private PaymentTransaction markOfflinePaymentAsPaid(
            Order order,
            PaymentIntent intent,
            PaymentTransaction transaction,
            LocalDateTime now
    ) {
        transaction.setStatus(STATUS_PAID);
        transaction.setPaidAt(now);
        transaction.setFailedReason(null);
        transaction = paymentTransactionRepository.save(transaction);

        intent.setStatus(STATUS_PAID);
        paymentIntentRepository.save(intent);

        order.setPaymentStatus(STATUS_PAID);
        orderRepository.save(order);

        return transaction;
    }

    private void validateOrderCanRecordOfflinePayment(Order order, String paymentMethod) {
        if (ORDER_CANCELLED.equals(order.getStatus()) || ORDER_REJECTED.equals(order.getStatus())) {
            throw new BaseException(ErrorCode.COM_004);
        }
        if (!paymentMethod.equals(order.getPaymentMethod())) {
            throw new BaseException(ErrorCode.COM_004);
        }
    }

    private PaymentIntent getOrCreateIntent(Order order, String paymentMethod) {
        return paymentIntentRepository.findByOrderAndProvider(order.getId(), paymentMethod)
                .orElseGet(() -> {
                    PaymentIntent intent = new PaymentIntent();
                    intent.setOrder(order);
                    intent.setProvider(paymentMethod);
                    intent.setAmount(order.getTotalAmount());
                    intent.setCurrency("VND");
                    intent.setStatus(STATUS_PENDING);
                    return paymentIntentRepository.save(intent);
                });
    }

    private PaymentTransaction createPendingTransaction(Order order, PaymentIntent intent, String paymentMethod) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrder(order);
        transaction.setPaymentIntent(intent);
        transaction.setTransactionCode(generateTransactionCode(paymentMethod));
        transaction.setProvider(paymentMethod);
        transaction.setPaymentMethod(paymentMethod);
        transaction.setAmount(order.getTotalAmount());
        transaction.setCurrency("VND");
        transaction.setStatus(STATUS_PENDING);
        return paymentTransactionRepository.save(transaction);
    }

    private PaymentTransaction createPaidTransactionForAlreadyPaidOrder(Order order, PaymentIntent intent, String paymentMethod, LocalDateTime now) {
        PaymentTransaction transaction = createPendingTransaction(order, intent, paymentMethod);
        transaction.setStatus(STATUS_PAID);
        transaction.setPaidAt(now);
        intent.setStatus(STATUS_PAID);
        paymentIntentRepository.save(intent);
        return paymentTransactionRepository.save(transaction);
    }

    private Optional<PaymentTransaction> findLatestTransaction(String orderId) {
        return paymentTransactionRepository.findLatestByOrder(orderId);
    }

    private String normalizeOfflineMethod(String paymentMethod) {
        String normalized = paymentMethod == null ? "" : paymentMethod.trim().toUpperCase();
        if (!METHOD_CASH.equals(normalized) && !METHOD_COD.equals(normalized)) {
            throw new BaseException(ErrorCode.COM_004);
        }
        return normalized;
    }

    private String generateTransactionCode(String paymentMethod) {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return "PAY-" + paymentMethod + "-" + System.currentTimeMillis() + "-" + random;
    }

    private PaymentTransactionResponse toResponse(PaymentTransaction transaction) {
        return paymentMapper.toResponse(transaction, STATUS_UNPAID);
    }
}
