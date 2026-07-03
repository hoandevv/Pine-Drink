package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.BranchVariantStockLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchVariantStockLogRepository extends JpaRepository<BranchVariantStockLog, String> {
    Page<BranchVariantStockLog> findByDailyStockId(String dailyStockId, Pageable pageable);
    Page<BranchVariantStockLog> findByOrderId(String orderId, Pageable pageable);
}
