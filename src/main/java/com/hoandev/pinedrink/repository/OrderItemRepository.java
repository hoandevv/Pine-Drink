package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.OrderItem;
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
}
