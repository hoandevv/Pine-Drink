package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.queue.event.email.PasswordResetEmailEvent;
import com.hoandev.pinedrink.queue.event.email.RegisterOtpEmailEvent;

/**
 * Service interface for sending email notifications.
 */
public interface EmailService {

    /**
     * Sends an OTP verification email to the recipient specified in the event.
     * When {@code app.email.enabled} is false, the email is skipped and logged.
     *
     * @param event the email event containing recipient, OTP, and template data
     */
    void sendOtpEmail(RegisterOtpEmailEvent event);

    /**
     * Sends a password reset OTP email to the recipient specified in the event.
     * When {@code app.email.enabled} is false, the email is skipped and logged.
     *
     * @param event the email event containing recipient, OTP, and template data
     */
    void sendPasswordResetOtpEmail(PasswordResetEmailEvent event);

    /**
     * Returns whether the email service is currently enabled.
     *
     * @return true if email sending is enabled, false otherwise
     */
    boolean isEnabled();
}
