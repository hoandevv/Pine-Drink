package com.hoandev.pinedrink.entity;

import com.hoandev.pinedrink.repository.result.InvoiceItemResult;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Entity
@SqlResultSetMapping(name = "InvoiceItemResultMapping", classes = @ConstructorResult(targetClass = InvoiceItemResult.class, columns = {
        @ColumnResult(name = "productName", type = String.class),
        @ColumnResult(name = "variantName", type = String.class),
        @ColumnResult(name = "quantity", type = Integer.class),
        @ColumnResult(name = "unitPrice", type = BigDecimal.class),
        @ColumnResult(name = "lineTotal", type = BigDecimal.class)
}))
@Table(name = "od_order_item")
@AttributeOverrides({
    @AttributeOverride(name = "createdBy", column = @Column(name = "created_by", insertable = false, updatable = false)),
    @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", insertable = false, updatable = false)),
    @AttributeOverride(name = "updatedBy", column = @Column(name = "updated_by", insertable = false, updatable = false))
})
public class OrderItem extends BaseEntity {

    @Column(name = "product_code", nullable = false)
    private String productCode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "variant_name")
    private String variantName;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "sugar_level", nullable = false)
    private String sugarLevel = "NORMAL";

    @Column(name = "ice_level", nullable = false)
    private String iceLevel = "NORMAL";

    private String note;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;
}
