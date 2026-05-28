package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "ia_account_role_assignment", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "role_id", "scope_id"}))
@AttributeOverrides({
    @AttributeOverride(name = "createdAt", column = @Column(name = "assigned_at", nullable = false, updatable = false)),
    @AttributeOverride(name = "createdBy", column = @Column(name = "assigned_by", columnDefinition = "CHAR(36)")),
    @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", insertable = false, updatable = false)),
    @AttributeOverride(name = "updatedBy", column = @Column(name = "updated_by", insertable = false, updatable = false))
})
public class AccountRoleAssignment extends BaseEntity {

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public LocalDateTime getAssignedAt() {
        return getCreatedAt();
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        setCreatedAt(assignedAt);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scope_id", nullable = false)
    private Scope scope;
}
