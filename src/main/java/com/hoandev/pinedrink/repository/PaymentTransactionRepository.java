package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.PaymentTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, String> {

    @Query("""
            select transaction
            from PaymentTransaction transaction
            where transaction.order.id = :orderId
              and transaction.paymentMethod = :paymentMethod
              and transaction.status = :status
            order by transaction.createdAt desc
            limit 1
            """)
    Optional<PaymentTransaction> findLatestByOrderAndMethodAndStatus(
            @Param("orderId") String orderId,
            @Param("paymentMethod") String paymentMethod,
            @Param("status") String status
    );

    Optional<PaymentTransaction> findByTransactionCodeAndPaymentMethod(
            String transactionCode,
            String paymentMethod
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select transaction
            from PaymentTransaction transaction
            where transaction.id = :transactionId
            """)
    Optional<PaymentTransaction> findByIdForUpdate(@Param("transactionId") String transactionId);

    @Query("""
            select transaction
            from PaymentTransaction transaction
            where transaction.order.id = :orderId
            order by transaction.createdAt desc
            limit 1
            """)
    Optional<PaymentTransaction> findLatestByOrder(@Param("orderId") String orderId);
}
