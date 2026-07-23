package com.hoandev.pinedrink.queue.publisher;

import com.hoandev.pinedrink.configuration.RabbitMqProperties;
import com.hoandev.pinedrink.queue.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Hiện thực {@link EventPublisher} dựa trên RabbitMQ.
 * <p>
 * Định tuyến sự kiện đến exchange email hoặc domain-events dựa theo loại sự kiện.
 * Tất cả sự kiện được tuần tự hóa thông qua message converter của {@link RabbitTemplate}
 * (được cấu hình trong {@link com.hoandev.pinedrink.configuration.RabbitMqConfig}).
 */
@Slf4j
@Component
public class RabbitMqEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqProperties properties;

    public RabbitMqEventPublisher(RabbitTemplate rabbitTemplate, RabbitMqProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Định tuyến được xác định theo tiền tố loại sự kiện:
     * <ul>
     *   <li>{@code EMAIL_*} -> email exchange</li>
     *   <li>các sự kiện còn lại -> domain-events exchange</li>
     * </ul>
     */
    @Override
    public void publish(DomainEvent event) {
        switch (event.eventType()) {
            case "EMAIL_OTP_VERIFICATION", "EMAIL_PASSWORD_RESET", "EMAIL_WELCOME" -> {
                var email = properties.email();
                log.debug("Publishing email event: {} to exchange={}, routingKey={}",
                        event.eventId(), email.exchange(), email.routingKey());
                rabbitTemplate.convertAndSend(email.exchange(), email.routingKey(), event);
            }
            default -> {
                var domain = properties.domainEvents();
                log.debug("Publishing domain event: {} to exchange={}, routingKey={}",
                        event.eventId(), domain.exchange(), domain.routingKey());
                rabbitTemplate.convertAndSend(domain.exchange(), domain.routingKey(), event);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(String exchange, String routingKey, DomainEvent event) {
        log.debug("Publishing event: {} to exchange={}, routingKey={}",
                event.eventId(), exchange, routingKey);
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}
