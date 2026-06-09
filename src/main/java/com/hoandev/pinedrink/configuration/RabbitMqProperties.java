package com.hoandev.pinedrink.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbitmq")
public record RabbitMqProperties(
        Stomp stomp,
        Realtime realtime,
        Channel domainEvents,
        Channel email,
        Channel backgroundJob
) {
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
}
