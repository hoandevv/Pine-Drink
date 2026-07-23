package com.hoandev.pinedrink.entity.dto.request.Chat;
/**
 * Request DTO for creating a chat room.
 */
public record CreateChatRoomRequest(
        String branchId,
        String orderId,
        String title
) {
}
