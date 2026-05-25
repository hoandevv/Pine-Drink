package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "pf_idempotency_key")
public class IdempotencyKey extends BaseEntity {

    @Column(name = "idempotency_key", unique = true, nullable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "response_body", columnDefinition = "json")
    private String responseBody;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
