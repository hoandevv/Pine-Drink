package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.OrderItem;
import com.hoandev.pinedrink.repository.projection.InvoiceItemProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, String> {
    List<OrderItem> findByOrderId(String orderId);

    @Query("SELECT i FROM OrderItem i LEFT JOIN FETCH i.variant WHERE i.order.id = :orderId")
    List<OrderItem> findByOrderIdWithVariant(@Param("orderId") String orderId);
    
    @Query("SELECT i FROM OrderItem i WHERE i.order.id IN :orderIds")
    List<OrderItem> findByOrderIdIn(@Param("orderIds") List<String> orderIds);

    @Query(value = """
            SELECT
                oi.product_name AS productName,
                oi.variant_name AS variantName,
                oi.quantity AS quantity,
                oi.unit_price AS unitPrice,
                oi.total_price AS lineTotal
            FROM od_order_item oi
            WHERE oi.order_id = :orderId
            ORDER BY oi.created_at ASC
            """, nativeQuery = true)
    List<InvoiceItemProjection> findInvoiceItemsByOrderId(@Param("orderId") String orderId);
}
