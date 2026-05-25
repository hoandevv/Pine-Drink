package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, String> {
    Optional<PaymentTransaction> findByTransactionCode(String transactionCode);
    List<PaymentTransaction> findByOrderId(String orderId);
}
