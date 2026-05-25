package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, String> {
    Optional<Cart> findByCustomerIdAndStatus(String customerId, String status);
    Optional<Cart> findBySessionIdAndStatus(String sessionId, String status);
}
