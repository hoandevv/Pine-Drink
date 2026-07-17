package com.hoandev.pinedrink.realtime;
/**
 * Interface for publishing realtime events.
 * là interface cho phép các service khác bắn event realtime vào hệ thống.
 */
public interface RealtimeEventPublisher {
    <T> void publish(String routingKey, RealtimeEvent<T> event);
}
