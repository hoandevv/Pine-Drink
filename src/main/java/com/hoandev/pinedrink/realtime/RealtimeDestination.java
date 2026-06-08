package com.hoandev.pinedrink.realtime;
/**
 * Utility class for defining realtime destinations.
 * là class gom toàn bộ đường dẫn WebSocket/STOMP destination về một chỗ.
 */
public final class RealtimeDestination {
    public static final String USER_NOTIFICATIONS = "/queue/notifications";
    public static final String USER_ORDERS = "/queue/orders";
    public static final String USER_CHAT = "/queue/chat";

    private RealtimeDestination() {
    }

    public static String orderTopic(String orderId) {
        return "/topic/orders/" + orderId;
    }

    public static String branchOrdersTopic(String branchId) {
        return "/topic/branches/" + branchId + "/orders";
    }

    public static String chatRoomTopic(String roomId) {
        return "/topic/chat/rooms/" + roomId;
    }

    public static String branchChatRoomsTopic(String branchId) {
        return "/topic/branches/" + branchId + "/chat/rooms";
    }
}
