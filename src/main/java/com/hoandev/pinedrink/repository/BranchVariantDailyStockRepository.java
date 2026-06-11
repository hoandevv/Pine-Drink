package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.BranchVariantDailyStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BranchVariantDailyStockRepository extends JpaRepository<BranchVariantDailyStock, String> {
    Optional<BranchVariantDailyStock> findByBranchIdAndVariantIdAndStockDate(String branchId, String variantId, LocalDate stockDate);
    List<BranchVariantDailyStock> findByBranchIdAndStockDate(String branchId, LocalDate stockDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from BranchVariantDailyStock s where s.id = :id")
    Optional<BranchVariantDailyStock> findByIdForUpdate(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from BranchVariantDailyStock s
            where s.branch.id = :branchId
              and s.variant.id = :variantId
              and s.stockDate = :stockDate
            """)
    Optional<BranchVariantDailyStock> findForUpdate(
            @Param("branchId") String branchId,
            @Param("variantId") String variantId,
            @Param("stockDate") LocalDate stockDate);
}
