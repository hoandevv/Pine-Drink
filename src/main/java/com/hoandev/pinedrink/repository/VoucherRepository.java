package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, String> {
    Optional<Voucher> findByBrandIdAndCode(String brandId, String code);
    List<Voucher> findByBrandIdAndStatus(String brandId, String status);
}
