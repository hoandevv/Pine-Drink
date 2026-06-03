package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.BranchProductAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchProductAvailabilityRepository extends JpaRepository<BranchProductAvailability, String> {
    List<BranchProductAvailability> findByBranchId(String branchId);
    List<BranchProductAvailability> findByProductId(String productId);
    Optional<BranchProductAvailability> findByBranchIdAndProductId(String branchId, String productId);
    Optional<BranchProductAvailability> findByIdAndBranchId(String id, String branchId);
    boolean existsByBranchIdAndProductId(String branchId, String productId);
    boolean existsByBranchIdAndProductIdAndIdNot(String branchId, String productId, String id);
}
