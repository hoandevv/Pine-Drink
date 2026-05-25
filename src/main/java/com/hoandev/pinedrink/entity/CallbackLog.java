package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "py_callback_log", uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "request_id"}))
public class CallbackLog extends BaseEntity {

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "transaction_code")
    private String transactionCode;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "raw_payload", columnDefinition = "json")
    private String rawPayload;

    @Column(name = "signature_valid", nullable = false)
    private boolean signatureValid = false;

    @Column(name = "processing_status", nullable = false)
    private String processingStatus = "PENDING";

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
