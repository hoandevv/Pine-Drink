package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.configuration.OrderProperties;
import com.hoandev.pinedrink.entity.Order;
import com.hoandev.pinedrink.entity.OrderStatusHistory;
import com.hoandev.pinedrink.entity.enums.OrderStatus;
import com.hoandev.pinedrink.repository.OrderItemRepository;
import com.hoandev.pinedrink.repository.OrderRepository;
import com.hoandev.pinedrink.repository.OrderStatusHistoryRepository;
import com.hoandev.pinedrink.service.BranchVariantDailyStockService;
import com.hoandev.pinedrink.service.OrderExpiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryServiceImpl implements OrderExpiryService {

    private static final String EXPIRED_REASON_TEMPLATE =
            "Order expired - not confirmed within %d minutes";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final BranchVariantDailyStockService dailyStockService;
    private final OrderProperties orderProperties;

    @Override
    @Transactional
    public void expire(String orderId) {
        log.info("Processing order expiry: orderId={}", orderId);

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElse(null);

        if (order == null) {
            log.warn("Order not found for expiry: orderId={}", orderId);
            return;
        }

        if (!OrderStatus.PENDING.getValue().equals(order.getStatus())) {
            log.info("Order {} already processed: status={}", orderId, order.getStatus());
            return;
        }

        Integer timeoutMinutes = orderProperties.getExpire().getTimeoutMinutes();
        String reason = EXPIRED_REASON_TEMPLATE.formatted(timeoutMinutes);

        order.setStatus(OrderStatus.REJECTED.getValue());
        order.setRejectedAt(java.time.LocalDateTime.now());
        order.setCancelReason(reason);

        releaseStock(order);
        saveStatusHistory(order, OrderStatus.PENDING, OrderStatus.REJECTED, reason);

        log.info("Order auto-rejected: orderId={}, orderCode={}, createdAt={}",
                order.getId(), order.getOrderCode(), order.getCreatedAt());
    }

    private void releaseStock(Order order) {
        LocalDate stockDate = order.getCreatedAt().toLocalDate();

        var items = orderItemRepository.findByOrderIdWithVariant(order.getId());
        for (var item : items) {
            if (item.getVariant() != null) {
                dailyStockService.release(
                        order.getBranch().getId(),
                        item.getVariant().getId(),
                        stockDate,
                        item.getQuantity(),
                        order.getId()
                );
            }
        }
    }

    private void saveStatusHistory(Order order, OrderStatus oldStatus,
                                   OrderStatus newStatus, String reason) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOldStatus(oldStatus.getValue());
        history.setNewStatus(newStatus.getValue());
        history.setReason(reason);
        orderStatusHistoryRepository.save(history);
    }
}
