package com.hoandev.pinedrink.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình RabbitMQ cho ứng dụng, ánh xạ từ các thuộc tính với tiền tố "app.rabbitmq".
 * Chứa các cấu hình kết nối STOMP, realtime và các channel (queue/exchange) khác.
 */
@ConfigurationProperties(prefix = "app.rabbitmq")
public record RabbitMqProperties(
        Stomp stomp,
        Realtime realtime,
        Channel domainEvents,
        Channel email,
        Channel report,
        OrderExpiry orderExpiry
) {
    /**
     * Cấu hình kết nối STOMP tới RabbitMQ.
     *
     * @param host địa chỉ host của RabbitMQ
     * @param port cổng kết nối
     * @param username tên người dùng
     * @param password mật khẩu
     * @param virtualHost virtual host (nếu có)
     * @param heartbeatSendInterval khoảng thời gian gửi heartbeat (ms)
     * @param heartbeatReceiveInterval khoảng thời gian nhận heartbeat (ms)
     */
    public record Stomp(
            String host,
            int port,
            String username,
            String password,
            String virtualHost,
            long heartbeatSendInterval,
            long heartbeatReceiveInterval
    ) {
    }

    /**
     * Cấu hình kênh realtime (exchange và các queue liên quan).
     *
     * @param exchange tên exchange dùng cho realtime
     * @param notificationQueue queue thông báo
     * @param auditQueue queue audit
     * @param chatQueue queue chat
     * @param webhookQueue queue webhook
     */
    public record Realtime(
            String exchange,
            String notificationQueue,
            String auditQueue,
            String chatQueue,
            String webhookQueue
    ) {
    }

    /**
     * Mô tả một kênh RabbitMQ cơ bản gồm exchange, queue và routing key.
     *
     * @param exchange tên exchange
     * @param queue tên queue chính
     * @param routingKey routing key chính
     */
    public record Channel(
            String exchange,
            String queue,
            String routingKey
    ) {
    }

    /**
     * Cấu hình cho việc xử lý order expiry (hết hạn đơn hàng).
     *
     * @param exchange exchange dùng cho order expiry
     * @param queue queue nhận sự kiện hết hạn
     * @param routingKey routing key cho message hết hạn
     */
    public record OrderExpiry(
            String exchange,
            String queue,
            String routingKey
    ) {
    }
}
