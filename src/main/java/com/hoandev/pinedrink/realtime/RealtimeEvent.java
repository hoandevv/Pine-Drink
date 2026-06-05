package com.hoandev.pinedrink.realtime;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
/**
 * Represents a realtime event.
 * là cái khung chuẩn của mọi message realtime mà backend bắn về client/admin qua WebSocket.
 */
@Setter
@Getter
public class RealtimeEvent<T> {
    private String eventId;
    private String type;
    private int version;
    private String actorId;
    private String targetType;
    private String targetId;
    private T payload;
    private Instant occurredAt;

}
