package com.hoandev.pinedrink.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbitmq")
public record RabbitMqProperties(
        Channel domainEvents,
        Channel email,
        Channel backgroundJob
) {
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
