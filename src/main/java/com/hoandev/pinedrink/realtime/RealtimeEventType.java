package com.hoandev.pinedrink.realtime;
/**
 * Utility class for defining realtime event types.
 * là class gom toàn bộ loại event realtime về một chỗ.
 */
public final class RealtimeEventType {
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_STATUS_CHANGED = "ORDER_STATUS_CHANGED";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";
    public static final String PAYMENT_STATUS_CHANGED = "PAYMENT_STATUS_CHANGED";
    public static final String CHAT_MESSAGE_SENT = "CHAT_MESSAGE_SENT";
    public static final String CHAT_ROOM_CREATED = "CHAT_ROOM_CREATED";
    public static final String CHAT_TYPING_STARTED = "CHAT_TYPING_STARTED";
    public static final String CHAT_TYPING_STOPPED = "CHAT_TYPING_STOPPED";
    public static final String CHAT_MESSAGE_READ = "CHAT_MESSAGE_READ";
    public static final String NOTIFICATION_CREATED = "NOTIFICATION_CREATED";
    public static final String BRANCH_ALERT = "BRANCH_ALERT";
    public static final String SYSTEM_ANNOUNCEMENT = "SYSTEM_ANNOUNCEMENT";

    private RealtimeEventType() {
    }
}
