package com.hoandev.pinedrink.queue.publisher;

import com.hoandev.pinedrink.configuration.OrderProperties;
import com.hoandev.pinedrink.entity.Order;
import com.hoandev.pinedrink.entity.enums.OrderStatus;
import com.hoandev.pinedrink.repository.OrderRepository;
import com.hoandev.pinedrink.service.OrderExpiryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@ConditionalOnProperty(
        name = "order.expire.fallback-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OrderExpiryFallbackPublisher {

    private final OrderRepository orderRepository;
    private final OrderExpiryService orderExpiryService;
    private final OrderProperties orderProperties;

    public OrderExpiryFallbackPublisher(OrderRepository orderRepository, OrderExpiryService orderExpiryService, OrderProperties orderProperties) {
        this.orderRepository = orderRepository;
        this.orderExpiryService = orderExpiryService;
        this.orderProperties = orderProperties;
    }

    @Scheduled(fixedDelay = 300000)
    public void handleExpiredOrders() {
        LocalDateTime cutoff = LocalDateTime.now()
                .minusMinutes(orderProperties.getExpire().getTimeoutMinutes());

        List<Order> expiredOrders = orderRepository
                .findByStatusAndCreatedAtBefore(
                        OrderStatus.PENDING.getValue(),
                        cutoff
                );

            log.info("Fallback expiry processing {} orders", expiredOrders.size());

        expiredOrders.forEach(order -> {
            try {
                orderExpiryService.expire(order.getId());
            } catch (Exception e) {
                log.error("Failed to expire order {}", order.getId(), e);
            }
        });
    }
}
