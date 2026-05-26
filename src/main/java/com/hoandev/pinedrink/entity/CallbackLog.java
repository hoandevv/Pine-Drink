package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "py_callback_log", uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "request_id"}))
@AttributeOverrides({
    @AttributeOverride(name = "createdAt", column = @Column(name = "received_at", nullable = false, updatable = false)),
    @AttributeOverride(name = "createdBy", column = @Column(name = "created_by", insertable = false, updatable = false)),
    @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", insertable = false, updatable = false)),
    @AttributeOverride(name = "updatedBy", column = @Column(name = "updated_by", insertable = false, updatable = false))
})
public class CallbackLog extends BaseEntity {

    @Column(nullable = false)
    private String provider;

    @Column(name = "transaction_code")
    private String transactionCode;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "raw_payload", columnDefinition = "json", nullable = false)
    private String rawPayload;

    @Column(name = "signature_valid", nullable = false)
    private boolean signatureValid = false;

    @Column(name = "processing_status", nullable = false)
    private String processingStatus = "PENDING";

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
