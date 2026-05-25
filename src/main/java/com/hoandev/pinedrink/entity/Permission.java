package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ia_permission")
public class Permission extends BaseEntity {

    @Column(unique = true)
    private String code;

    private String name;

    private String module;

    private String description;
}
