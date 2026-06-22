package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, String> {
    Optional<Cart> findByCustomerIdAndBranchIdAndStatus(String customerId, String branchId, String status);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cart c WHERE c.customer.id = :customerId AND c.branch.id = :branchId AND c.status = :status")
    Optional<Cart> findByCustomerIdAndBranchIdAndStatusForUpdate(
            @Param("customerId") String customerId,
            @Param("branchId") String branchId,
            @Param("status") String status);
}
