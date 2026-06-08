package com.hoandev.pinedrink.realtime;
/**
 * Interface for publishing realtime events.
 * là interface cho phép các service khác bắn event realtime vào hệ thống.
 */
public interface RealtimePublishService {
    <T> void publishToTopic(String destination, RealtimeEvent<T> event);
    <T> void publishToUser(String accountId, String destination, RealtimeEvent<T> event);
    <T> void publishOrderEvent(String orderId, RealtimeEvent<T> event);
    <T> void publishBranchOrderEvent(String branchId, RealtimeEvent<T> event);
    <T> void publishChatRoomEvent(String roomId, RealtimeEvent<T> event);
    <T> void publishBranchChatRoomEvent(String branchId, RealtimeEvent<T> event);
}
