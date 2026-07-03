package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Voucher;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, String> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voucher v WHERE v.code = :code")
    Optional<Voucher> findByCodeForUpdate(@Param("code") String code);

    @Query("""
            select v from Voucher v
            where (:keyword is null
                    or lower(v.code) like lower(concat('%', :keyword, '%'))
                    or lower(v.name) like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(v.description, '')) like lower(concat('%', :keyword, '%')))
              and (:status is null or v.status = :status)
              and (:discountType is null or v.discountType = :discountType)
              and (:activeAt is null or (v.startAt <= :activeAt and v.endAt >= :activeAt))
              and (:branchId is null
                    or exists (select 1 from VoucherBranch vb
                               where vb.voucher.id = v.id and vb.branch.id = :branchId)
                    or not exists (select 1 from VoucherBranch vb2 where vb2.voucher.id = v.id))
            """)
    Page<Voucher> search(@Param("keyword") String keyword,
                         @Param("status") String status,
                         @Param("discountType") String discountType,
                         @Param("branchId") String branchId,
                         @Param("activeAt") java.time.LocalDateTime activeAt,
                         Pageable pageable);
}
