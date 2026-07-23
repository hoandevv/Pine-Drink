package com.hoandev.pinedrink.realtime;
/**
 * Utility class for defining realtime event types.
 * là class gom toàn bộ loại event realtime về một chỗ.
 */
public final class RealtimeEventType {
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_STATUS_CHANGED = "ORDER_STATUS_CHANGED";
    public static final String CHAT_MESSAGE_SENT = "CHAT_MESSAGE_SENT";
    public static final String CHAT_ROOM_CREATED = "CHAT_ROOM_CREATED";

    private RealtimeEventType() {
    }
}
