package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ce_setting", uniqueConstraints = @UniqueConstraint(columnNames = {"brand_id", "branch_id", "config_key"}))
public class Setting extends BaseEntity {

    @Column(name = "config_key", nullable = false)
    private String configKey;

    @Column(name = "config_value", nullable = false)
    private String configValue;

    @Column(name = "data_type", nullable = false)
    private String dataType;

    private String description;

    @Column(name = "is_runtime_editable", nullable = false)
    private boolean isRuntimeEditable = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;
}
