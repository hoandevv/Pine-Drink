package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    Optional<Order> findByOrderCode(String orderCode);
    List<Order> findByBranchIdAndStatusOrderByCreatedAtDesc(String branchId, String status);
    List<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
