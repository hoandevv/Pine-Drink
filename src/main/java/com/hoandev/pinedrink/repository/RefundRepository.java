package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, String> {
    Optional<Refund> findByRefundCode(String refundCode);
    Optional<Refund> findByTransactionId(String transactionId);
}
