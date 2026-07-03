package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, String> {
    /**
     * Counts the number of usage records for a given voucher and customer.
     *
     * @param voucherId the ID of the voucher
     * @param customerId the ID of the customer
     * @return the number of usage records
     */
    long countByVoucherIdAndCustomerId(String voucherId, String customerId);
}
