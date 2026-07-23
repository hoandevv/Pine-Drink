package com.hoandev.pinedrink.realtime.payload;

/**
 * Payload for order status changed events.
 */
public record OrderStatusChangedPayload(
        String orderId,
        String orderCode,
        String oldStatus,
        String newStatus,
        String reason,
        String branchId,
        String customerAccountId
) {
}
