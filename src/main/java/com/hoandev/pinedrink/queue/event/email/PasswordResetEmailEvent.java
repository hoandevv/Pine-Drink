package com.hoandev.pinedrink.queue.event.email;

import com.hoandev.pinedrink.queue.event.DomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Event fired when a password-reset link/token needs to be delivered via email.
 * <p>
 * The {@code resetToken} is included in {@code templateData} for Thymeleaf rendering;
 * it must never be logged or exposed in plain text outside the email pipeline.
 *
 * @param to           recipient email address
 * @param subject      email subject line
 * @param templateName Thymeleaf template name (without extension)
 * @param templateData variables injected into the template (includes {@code resetToken})
 * @param metadata     auxiliary metadata for routing / tracing
 */
public record PasswordResetEmailEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String source,
        String to,
        String subject,
        String templateName,
        Map<String, Object> templateData,
        Map<String, Object> metadata
) implements DomainEvent {

    /**
     * Factory method that populates standard fields with defaults for the
     * password-reset flow.
     *
     * @param to            recipient email
     * @param otp           the OTP code (will be placed in template data)
     * @param expiryMinutes OTP validity duration shown in the email
     */
    public static PasswordResetEmailEvent of(String to, String otp, int expiryMinutes) {
        return new PasswordResetEmailEvent(
                UUID.randomUUID().toString(),
                "EMAIL_PASSWORD_RESET",
                Instant.now(),
                "auth-service",
                to,
                "Đặt lại mật khẩu - Pine Drink",
                "password-reset",
                Map.of("otp", otp, "expiryMinutes", expiryMinutes),
                Map.of()
        );
    }

    @Override
    public String eventId() { return eventId; }

    @Override
    public String eventType() { return eventType; }

    @Override
    public Instant occurredAt() { return occurredAt; }

    @Override
    public String source() { return source; }
}
