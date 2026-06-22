package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, String> {
    /**
     * Checks if a voucher usage record exists for the given voucher ID.
     *
     * @param voucherId the ID of the voucher
     * @return true if a usage record exists, false otherwise
     */
    boolean existsByVoucherId(String voucherId);
    /**
     * Counts the number of usage records for a given voucher and customer.
     *
     * @param voucherId the ID of the voucher
     * @param customerId the ID of the customer
     * @return the number of usage records
     */
    long countByVoucherIdAndCustomerId(String voucherId, String customerId);
}
