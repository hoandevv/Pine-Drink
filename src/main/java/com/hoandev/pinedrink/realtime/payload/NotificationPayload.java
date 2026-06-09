package com.hoandev.pinedrink.realtime.payload;
/**
 * Payload for notification events.
 */
public record NotificationPayload(
        String title,
        String message,
        String severity,
        String targetUrl
) {
}
