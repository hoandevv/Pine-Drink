package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.BranchProductAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchProductAvailabilityRepository extends JpaRepository<BranchProductAvailability, String> {
    List<BranchProductAvailability> findByBranchId(String branchId);

    @Query("""
            SELECT a FROM BranchProductAvailability a
            WHERE a.branch.id = :branchId
              AND a.product.status = :productStatus
              AND a.product.category.status = :categoryStatus
            """)
    List<BranchProductAvailability> findPublicByBranchId(
            @Param("branchId") String branchId,
            @Param("productStatus") String productStatus,
            @Param("categoryStatus") String categoryStatus
    );

    Optional<BranchProductAvailability> findByIdAndBranchId(String id, String branchId);

    @Query("""
            SELECT a FROM BranchProductAvailability a
            WHERE a.id = :id
              AND a.branch.id = :branchId
              AND a.product.status = :productStatus
              AND a.product.category.status = :categoryStatus
            """)
    Optional<BranchProductAvailability> findPublicByIdAndBranchId(
            @Param("id") String id,
            @Param("branchId") String branchId,
            @Param("productStatus") String productStatus,
            @Param("categoryStatus") String categoryStatus
    );

    boolean existsByBranchIdAndProductId(String branchId, String productId);
    boolean existsByBranchIdAndProductIdAndIdNot(String branchId, String productId, String id);
}
