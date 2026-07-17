package com.hoandev.pinedrink.realtime.payload;

import java.math.BigDecimal;
/**
 * Payload for order created events.
 */
public record OrderCreatedPayload(
        String orderId,
        String orderCode,
        String branchId,
        String customerAccountId,
        BigDecimal totalAmount
) {
}
