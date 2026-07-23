package com.hoandev.pinedrink.entity.dto.response.Chat;

import java.time.LocalDateTime;
/**
 * A record representing the response for a chat message.
 */
public record ChatMessageResponse(
        String id,
        String roomId,
        String senderAccountId,
        String senderType,
        String senderName,
        String messageType,
        String content,
        String metadata,
        String status,
        LocalDateTime createdAt
) {
}
