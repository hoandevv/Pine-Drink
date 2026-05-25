package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ia_role")
public class Role extends BaseEntity {

    @Column(unique = true)
    private String code;

    private String name;

    private String description;

    @Column(name = "role_type")
    private String roleType = "SYSTEM";
}
