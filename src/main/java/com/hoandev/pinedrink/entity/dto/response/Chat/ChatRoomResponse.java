package com.hoandev.pinedrink.entity.dto.response.Chat;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        String id,
        String roomCode,
        String roomType,
        String customerAccountId,
        String customerName,
        String assignedStaffAccountId,
        String assignedStaffName,
        String branchId,
        String orderId,
        String title,
        String lastMessagePreview,
        LocalDateTime lastMessageAt,
        String status,
        LocalDateTime createdAt
) {
}
