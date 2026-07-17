package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.BranchToppingAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchToppingAvailabilityRepository extends JpaRepository<BranchToppingAvailability, String> {
    List<BranchToppingAvailability> findByBranchId(String branchId);
    Optional<BranchToppingAvailability> findByIdAndBranchId(String id, String branchId);
    boolean existsByBranchIdAndToppingId(String branchId, String toppingId);
    boolean existsByBranchIdAndToppingIdAndIdNot(String branchId, String toppingId, String id);
}
