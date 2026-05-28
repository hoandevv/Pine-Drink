package com.hoandev.pinedrink.queue.publisher;

import com.hoandev.pinedrink.queue.event.DomainEvent;

/**
 * Abstraction for publishing domain events to the messaging infrastructure.
 * <p>
 * Implementations route events to exchanges based on event type
 * (see {@link com.hoandev.pinedrink.queue.publisher.RabbitMqEventPublisher}).
 */
public interface EventPublisher {

    /**
     * Publishes an event, automatically resolving the target exchange and routing key
     * from the event type.
     */
    void publish(DomainEvent event);

    /**
     * Publishes an event to a specific exchange and routing key,
     * bypassing automatic routing resolution.
     */
    void publish(String exchange, String routingKey, DomainEvent event);
}
