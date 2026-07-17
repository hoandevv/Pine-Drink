package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "nt_template", uniqueConstraints = @UniqueConstraint(columnNames = {"template_code", "channel"}))
public class Template extends BaseEntity {

    @Column(name = "template_code", nullable = false)
    private String templateCode;

    @Column(nullable = false)
    private String channel;

    private String subject;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    @Column(columnDefinition = "json")
    private String variables;

}
