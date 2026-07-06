package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.VoucherBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface VoucherBranchRepository extends JpaRepository<VoucherBranch, String> {
    /**
     * Finds voucher branches by a collection of voucher IDs.
     *
     * @param voucherIds the collection of voucher IDs
     * @return the list of voucher branches
     */
    List<VoucherBranch> findByVoucherIdIn(Collection<String> voucherIds);

    boolean existsByVoucherId(String voucherId);

    boolean existsByVoucherIdAndBranchId(String voucherId, String branchId);

    /**
     * Deletes voucher branches by voucher ID.
     *
     * @param voucherId the ID of the voucher
     */
    void deleteByVoucherId(String voucherId);
}
