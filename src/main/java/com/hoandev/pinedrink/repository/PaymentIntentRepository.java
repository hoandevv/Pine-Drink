package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.PaymentIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, String> {
    Optional<PaymentIntent> findByOrderId(String orderId);
}
