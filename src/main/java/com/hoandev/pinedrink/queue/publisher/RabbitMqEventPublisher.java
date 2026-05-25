package com.hoandev.pinedrink.queue.publisher;

import com.hoandev.pinedrink.configuration.RabbitMqProperties;
import com.hoandev.pinedrink.queue.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ-backed implementation of {@link EventPublisher}.
 * <p>
 * Routes events to the email or domain-events exchange based on the event type.
 * All events are serialized via the {@link RabbitTemplate}'s message converter
 * (configured in {@link com.hoandev.pinedrink.configuration.RabbitMqConfig}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqProperties properties;

    /**
     * {@inheritDoc}
     * <p>
     * Routing is determined by event type prefix:
     * <ul>
     *   <li>{@code EMAIL_*} → email exchange</li>
     *   <li>all others → domain-events exchange</li>
     * </ul>
     */
    @Override
    public void publish(DomainEvent event) {
        switch (event.eventType()) {
            case "EMAIL_OTP_VERIFICATION", "EMAIL_RESET_PASSWORD", "EMAIL_WELCOME" -> {
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
