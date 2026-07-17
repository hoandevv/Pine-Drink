package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.OrderDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderDeliveryRepository extends JpaRepository<OrderDelivery, String> {
    Optional<OrderDelivery> findByOrderId(String orderId);
}
