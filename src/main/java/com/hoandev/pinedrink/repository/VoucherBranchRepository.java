package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.VoucherBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherBranchRepository extends JpaRepository<VoucherBranch, String> {
    List<VoucherBranch> findByVoucherId(String voucherId);
    List<VoucherBranch> findByBranchId(String branchId);
}
