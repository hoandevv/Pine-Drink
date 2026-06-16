package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, String> {
    /**
        * Find the active cart for a specific customer and branch.
     */
    Optional<Cart> findByCustomerIdAndBranchIdAndStatus(String customerId, String branchId, String status);
}
