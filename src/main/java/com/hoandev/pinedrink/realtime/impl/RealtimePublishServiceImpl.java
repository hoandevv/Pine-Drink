package com.hoandev.pinedrink.realtime.impl;

import com.hoandev.pinedrink.realtime.RealtimeDestination;
import com.hoandev.pinedrink.realtime.RealtimeEvent;
import com.hoandev.pinedrink.realtime.RealtimePublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
/**
 * Implementation of the realtime publish service.
 * Service trung gian để bắn realtime event từ backend ra client qua WebSocket/STOMP.
 *
 * Service xử lý nghiệp vụ
 *         ↓
 * RealtimePublishServiceImpl
 *         ↓
 * SimpMessagingTemplate
 *         ↓
 * WebSocket/STOMP broker
 *         ↓
 * Client/Admin đang subscribe nhận event
 */
@Service
@RequiredArgsConstructor
public class RealtimePublishServiceImpl implements RealtimePublishService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public <T> void publishToTopic(String destination, RealtimeEvent<T> event) {
        messagingTemplate.convertAndSend(destination, event);
    }

    @Override
    public <T> void publishToUser(String accountId, String destination, RealtimeEvent<T> event) {
        messagingTemplate.convertAndSendToUser(accountId, destination, event);
    }

    @Override
    public <T> void publishOrderEvent(String orderId, RealtimeEvent<T> event) {
        publishToTopic(RealtimeDestination.orderTopic(orderId), event);
    }

    @Override
    public <T> void publishBranchOrderEvent(String branchId, RealtimeEvent<T> event) {
        publishToTopic(RealtimeDestination.branchOrdersTopic(branchId), event);
    }

    @Override
    public <T> void publishChatRoomEvent(String roomId, RealtimeEvent<T> event) {
        publishToTopic(RealtimeDestination.chatRoomTopic(roomId), event);
    }

    @Override
    public <T> void publishBranchChatRoomEvent(String branchId, RealtimeEvent<T> event) {
        publishToTopic(RealtimeDestination.branchChatRoomsTopic(branchId), event);
    }
}
