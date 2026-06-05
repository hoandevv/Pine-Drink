package com.hoandev.pinedrink.realtime.payload;

import java.time.Instant;
/**
 * Payload for chat message events.
 * Payload là phần dữ liệu đi kèm trong event/message được bắn vào queue để consumer xử lý.
 */
public record ChatMessagePayload(
        String roomId,
        String messageId,
        String senderId,
        String senderName,
        String content,
        Instant sentAt
) {
}
