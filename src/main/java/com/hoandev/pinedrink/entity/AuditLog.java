package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ia_audit_log")
@AttributeOverrides({
    @AttributeOverride(name = "createdBy", column = @Column(name = "created_by", insertable = false, updatable = false)),
    @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", insertable = false, updatable = false)),
    @AttributeOverride(name = "updatedBy", column = @Column(name = "updated_by", insertable = false, updatable = false))
})
public class AuditLog extends BaseEntity {

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String module;

    @Column(name = "target_type")
    private String targetType;

    @Column(name = "target_id", columnDefinition = "CHAR(36)")
    private String targetId;

    @Column(name = "brand_id", columnDefinition = "CHAR(36)")
    private String brandId;

    @Column(name = "branch_id", columnDefinition = "CHAR(36)")
    private String branchId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "before_data", columnDefinition = "json")
    private String beforeData;

    @Column(name = "after_data", columnDefinition = "json")
    private String afterData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_account_id")
    private Account actorAccount;
}
