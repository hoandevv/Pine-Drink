package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "pr_category", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class Category extends BaseEntity {

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @OneToMany(mappedBy = "category")
    private List<Product> products;
}
