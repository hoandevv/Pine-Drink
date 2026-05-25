package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ia_scope", uniqueConstraints = @UniqueConstraint(columnNames = {"scope_type", "brand_id", "branch_id"}))
public class Scope extends BaseEntity {

    @Column(name = "scope_type")
    private String scopeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;
}
