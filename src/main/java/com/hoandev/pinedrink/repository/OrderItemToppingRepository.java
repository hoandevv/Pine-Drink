package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.OrderItemTopping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemToppingRepository extends JpaRepository<OrderItemTopping, String> {

    @Query("SELECT t FROM OrderItemTopping t WHERE t.orderItem.id IN :orderItemIds")
    List<OrderItemTopping> findByOrderItemIdIn(@Param("orderItemIds") List<String> orderItemIds);
}
