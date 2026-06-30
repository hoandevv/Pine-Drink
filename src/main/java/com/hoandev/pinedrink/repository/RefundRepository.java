package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, String> {
    Optional<Refund> findByRefundCode(String refundCode);

    @Query("""
            select refund
            from Refund refund
            where refund.transaction.id = :transactionId
            """)
    Optional<Refund> findByTransaction(@Param("transactionId") String transactionId);
}
