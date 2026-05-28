package com.hoandev.pinedrink.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(30)")
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "created_by", columnDefinition = "CHAR(36)")
    private String createdBy;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "updated_by", columnDefinition = "CHAR(36)")
    private String updatedBy;

    @PrePersist
    protected void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
