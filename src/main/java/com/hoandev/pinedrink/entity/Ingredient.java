package com.hoandev.pinedrink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "iv_ingredient", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class Ingredient extends BaseEntity {

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String unit;

    @Column(name = "min_stock_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal minStockQuantity = BigDecimal.ZERO;

}
