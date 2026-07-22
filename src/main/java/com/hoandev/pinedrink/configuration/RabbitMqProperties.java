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
        ReportChannel report,
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
     * @param dlq dead-letter queue
     * @param dlx dead-letter exchange
     */
    public record Realtime(
            String exchange,
            String notificationQueue,
            String auditQueue,
            String chatQueue,
            String webhookQueue,
            String dlq,
            String dlx
    ) {
    }

    /**
     * Mô tả một kênh (channel) tổng quát gồm exchange, queue và các thiết lập retry/dlq.
     *
     * @param exchange tên exchange
     * @param queue tên queue chính
     * @param retryQueue tên queue retry
     * @param dlq tên dead-letter queue
     * @param routingKey routing key chính
     * @param retryRoutingKey routing key cho retry
     * @param dlqRoutingKey routing key cho dlq
     * @param retryTtlMs thời gian TTL cho tin nhắn retry (ms)
     * @param concurrentConsumers số consumer chạy đồng thời ban đầu
     * @param maxConsumers số consumer tối đa
     * @param prefetch số message prefetch cho consumer
     */
    public record Channel(
            String exchange,
            String queue,
            String retryQueue,
            String dlq,
            String routingKey,
            String retryRoutingKey,
            String dlqRoutingKey,
            long retryTtlMs,
            int concurrentConsumers,
            int maxConsumers,
            int prefetch
    ) {
    }

    /**
     * Cấu hình tối thiểu cho kênh xuất báo cáo.
     *
     * @param exchange tên exchange
     * @param queue tên queue nhận job báo cáo
     * @param routingKey routing key publish job báo cáo
     */
    public record ReportChannel(
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
