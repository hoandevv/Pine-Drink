package com.hoandev.pinedrink.queue.consumer;

import com.hoandev.pinedrink.queue.event.email.PasswordResetEmailEvent;
import com.hoandev.pinedrink.queue.event.email.RegisterOtpEmailEvent;
import com.hoandev.pinedrink.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ consumer that listens on the email queue and delegates
 * to {@link EmailService} for actual message rendering and delivery.
 * <p>
 * Each event type has a dedicated {@link RabbitHandler} method.
 * Message deserialization is handled by the converter configured in
 * {@link com.hoandev.pinedrink.configuration.RabbitMqConfig}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = "${app.rabbitmq.email.queue}")
public class EmailConsumer {

    private final EmailService emailService;

    /**
     * Handles registration OTP email events by delegating to {@link EmailService#sendOtpEmail}.
     */
    @RabbitHandler
    public void handleRegisterOtp(RegisterOtpEmailEvent event) {
        log.debug("Received email event: type={}, to={}", event.eventType(), event.to());
        emailService.sendOtpEmail(event);
    }

    /**
     * Handles password-reset email events.
     * <p>
     * TODO: Implement password-reset email sending logic.
     */
    @RabbitHandler
    public void handlePasswordReset(PasswordResetEmailEvent event) {
        log.debug("Received password reset event: to={}", event.to());
    }
}
