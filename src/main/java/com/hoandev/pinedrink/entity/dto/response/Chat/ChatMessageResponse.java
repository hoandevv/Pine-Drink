package com.hoandev.pinedrink.entity.dto.response.Chat;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        String id,
        String roomId,
        String senderAccountId,
        String senderName,
        String messageType,
        String content,
        String metadata,
        String status,
        LocalDateTime createdAt
) {
}
