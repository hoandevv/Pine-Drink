package com.hoandev.pinedrink.entity.dto.request.Chat;

import jakarta.validation.constraints.NotBlank;
/**
 * Request DTO for sending a chat message.
 */
public record SendChatMessageRequest(
        @NotBlank String roomId,
        String messageType,
        String content,
        String metadata
) {
}
