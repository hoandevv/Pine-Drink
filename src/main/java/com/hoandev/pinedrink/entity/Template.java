package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "nt_template", uniqueConstraints = @UniqueConstraint(columnNames = {"brand_id", "template_code", "channel"}))
public class Template extends BaseEntity {

    @Column(name = "template_code", nullable = false)
    private String templateCode;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "subject")
    private String subject;

    @Lob
    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "variables", columnDefinition = "json")
    private String variables;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;
}
