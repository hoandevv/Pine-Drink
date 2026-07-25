package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface RefundRepository extends JpaRepository<Refund, String> {

    @Query("""
            select coalesce(sum(refund.amount), 0)
            from Refund refund
            where refund.transaction.id = :transactionId
              and refund.status <> 'FAILED'
            """)
    BigDecimal sumActiveRefundAmountByTransaction(@Param("transactionId") String transactionId);
}
