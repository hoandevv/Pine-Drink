package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, String> {
    List<VoucherUsage> findByVoucherId(String voucherId);
    List<VoucherUsage> findByOrderId(String orderId);
    List<VoucherUsage> findByCustomerIdAndVoucherId(String customerId, String voucherId);
}
