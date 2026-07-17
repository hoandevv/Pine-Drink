package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.PaymentIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, String> {

    @Query("""
            select intent
            from PaymentIntent intent
            where intent.order.id = :orderId
              and intent.provider = :provider
            """)
    Optional<PaymentIntent> findByOrderAndProvider(
            @Param("orderId") String orderId,
            @Param("provider") String provider
    );
}
