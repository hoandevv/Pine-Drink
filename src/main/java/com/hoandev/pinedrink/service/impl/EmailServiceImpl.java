package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.exception.EmailSendException;
import com.hoandev.pinedrink.queue.event.email.PasswordResetEmailEvent;
import com.hoandev.pinedrink.queue.event.email.RegisterOtpEmailEvent;
import com.hoandev.pinedrink.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

/**
 * Default implementation of {@link EmailService}.
 * Renders Thymeleaf templates and sends emails via JavaMailSender.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.email.from:noreply@pine-drink.com}")
    private String from;

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendOtpEmail(RegisterOtpEmailEvent event) {
        validateOtpEmailEvent(event);

        if (!emailEnabled) {
            log.info("[EMAIL_DISABLED] OTP email skipped to={}, subject={}",
                    event.to(), event.subject());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(event.to());
            helper.setSubject(event.subject());

            String otp = getString(event.templateData(), "otp", "");
            int expiry = getInt(event.templateData(), "expiryMinutes", 5);

            Context context = new Context();
            context.setVariable("otp", otp);
            context.setVariable("expiryMinutes", expiry);

            String html = templateEngine.process("email/register-otp", context);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("OTP email sent to={}", event.to());
        } catch (Exception e) {
            log.error("Failed to send OTP email to={}", event.to(), e);
            throw new EmailSendException("Failed to send OTP email", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendPasswordResetOtpEmail(PasswordResetEmailEvent event) {
        validatePasswordResetEmailEvent(event);

        if (!emailEnabled) {
            log.info("[EMAIL_DISABLED] Password reset OTP email skipped to={}, subject={}",
                    event.to(), event.subject());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(event.to());
            helper.setSubject(event.subject());

            String otp = getString(event.templateData(), "otp", "");
            int expiry = getInt(event.templateData(), "expiryMinutes", 5);

            Context context = new Context();
            context.setVariable("otp", otp);
            context.setVariable("expiryMinutes", expiry);

            String html = templateEngine.process("email/password-reset", context);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Password reset OTP email sent to={}", event.to());
        } catch (Exception e) {
            log.error("Failed to send password reset OTP email to={}", event.to(), e);
            throw new EmailSendException("Failed to send password reset OTP email", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return emailEnabled;
    }

    // ──────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────

    /**
     * Validates that the email event has valid recipient and subject.
     */
    private void validateOtpEmailEvent(RegisterOtpEmailEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Email event must not be null");
        }
        if (event.to() == null || event.to().isBlank()) {
            throw new IllegalArgumentException("Email recipient must not be blank");
        }
        if (event.subject() == null || event.subject().isBlank()) {
            throw new IllegalArgumentException("Email subject must not be blank");
        }
    }

    /**
     * Validates that the password reset email event has valid recipient and subject.
     */
    private void validatePasswordResetEmailEvent(PasswordResetEmailEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Email event must not be null");
        }
        if (event.to() == null || event.to().isBlank()) {
            throw new IllegalArgumentException("Email recipient must not be blank");
        }
        if (event.subject() == null || event.subject().isBlank()) {
            throw new IllegalArgumentException("Email subject must not be blank");
        }
    }

    /**
     * Safely extracts a string value from template data, falling back to a default.
     */
    private String getString(Map<String, Object> data, String key, String defaultValue) {
        Object value = data != null ? data.get(key) : null;
        return value != null ? String.valueOf(value) : defaultValue;
    }

    /**
     * Safely extracts an integer value from template data,
     * handling Number, String, and null types gracefully.
     */
    private int getInt(Map<String, Object> data, String key, int defaultValue) {
        Object value = data != null ? data.get(key) : null;
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
