package com.hoandev.pinedrink.realtime.impl;

import com.hoandev.pinedrink.configuration.RabbitMqProperties;
import com.hoandev.pinedrink.realtime.RealtimeEvent;
import com.hoandev.pinedrink.realtime.RealtimeEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
/**
 * Implementation of the realtime event publisher using RabbitMQ.
 * bắn event vào RabbitMQ trước.
 * Ví dụ
 * OrderService
 *    ↓
 * RabbitRealtimeEventPublisher
 *    ↓
 * RabbitMQ
 *    ↓
 * Realtime listener/consumer
 *    ↓
 * WebSocket/STOMP
 *    ↓
 * Client
 */
@Service
@RequiredArgsConstructor
public class RabbitRealtimeEventPublisher implements RealtimeEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqProperties rabbitMqProperties;

    @Override
    public <T> void publish(String routingKey, RealtimeEvent<T> event) {
        rabbitTemplate.convertAndSend(rabbitMqProperties.realtime().exchange(), routingKey, event);
    }
}
