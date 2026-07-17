package com.hoandev.pinedrink.queue.listener;

import com.hoandev.pinedrink.service.OrderExpiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
/**
 * Listener cho thông báo hết hạn đơn hàng từ RabbitMQ.
 *
 * Ngắn gọn: lắng nghe message chứa orderId và gọi {@link OrderExpiryService#expire(String)}
 * để xử lý việc đánh dấu đơn hàng là đã hết hạn.
 */
public class OrderExpiryListener {

    /** Service chứa logic để đánh dấu đơn hàng là hết hạn. */
    private final OrderExpiryService orderExpiryService;

    @RabbitListener(
            queues = "${app.rabbitmq.order-expiry.queue:pine-drink.order-expire.queue}",
            concurrency = "1-3"
    )
    /**
     * Xử lý message hết hạn đơn hàng.
     *
     * @param orderId ID của đơn hàng (chuỗi) nhận từ message
     */
    public void handleOrderExpiry(String orderId) {
        log.info("Received order expiry message: orderId={}", orderId);
        // Gọi service để đánh dấu đơn hàng là đã hết hạn
        orderExpiryService.expire(orderId);
    }
}
