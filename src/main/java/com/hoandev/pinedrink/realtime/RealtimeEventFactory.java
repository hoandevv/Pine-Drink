package com.hoandev.pinedrink.realtime;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
/**
 * Factory for creating realtime events.
 * class chuyên tạo RealtimeEvent chuẩn, để các service khác không phải tự set từng field lặp đi lặp lại.
 */
@Component
public class RealtimeEventFactory {

    public <T> RealtimeEvent<T> create(String type, String actorId,
                                       String targetType, String targetId, T payload) {
        RealtimeEvent<T> event = new RealtimeEvent<>();
        event.setEventId(UUID.randomUUID().toString());
        event.setType(type);
        event.setVersion(1);
        event.setActorId(actorId);
        event.setTargetType(targetType);
        event.setTargetId(targetId);
        event.setPayload(payload);
        event.setOccurredAt(Instant.now());
        return event;
    }
}
