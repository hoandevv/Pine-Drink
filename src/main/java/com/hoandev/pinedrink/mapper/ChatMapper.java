package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.Account;
import com.hoandev.pinedrink.entity.ChatMessage;
import com.hoandev.pinedrink.entity.ChatRoom;
import com.hoandev.pinedrink.entity.dto.response.Chat.ChatMessageResponse;
import com.hoandev.pinedrink.entity.dto.response.Chat.ChatRoomResponse;
import org.springframework.stereotype.Component;

@Component
public class ChatMapper {

    public ChatRoomResponse toRoomResponse(ChatRoom room) {
        if (room == null) {
            return null;
        }
        Account customer = room.getCustomerAccount();
        Account staff = room.getAssignedStaffAccount();
        return new ChatRoomResponse(
                room.getId(),
                room.getRoomCode(),
                room.getRoomType(),
                customer != null ? customer.getId() : null,
                customer != null ? customer.getFullName() : null,
                staff != null ? staff.getId() : null,
                staff != null ? staff.getFullName() : null,
                room.getBranch() != null ? room.getBranch().getId() : null,
                room.getOrder() != null ? room.getOrder().getId() : null,
                room.getTitle(),
                room.getLastMessagePreview(),
                room.getLastMessageAt(),
                room.getStatus(),
                room.getCreatedAt()
        );
    }

    public ChatMessageResponse toMessageResponse(ChatMessage message) {
        if (message == null) {
            return null;
        }
        Account sender = message.getSenderAccount();
        return new ChatMessageResponse(
                message.getId(),
                message.getRoom().getId(),
                sender != null ? sender.getId() : null,
                message.getSenderType(),
                sender != null ? sender.getFullName() : null,
                message.getMessageType(),
                message.getContent(),
                message.getMetadata(),
                message.getStatus(),
                message.getCreatedAt()
        );
    }
}
