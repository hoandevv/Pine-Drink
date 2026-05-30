package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, String> {
    Page<Branch> findByBrandId(String brandId, Pageable pageable);
    Page<Branch> findByBrandIdAndStatus(String brandId, String status, Pageable pageable);
    Optional<Branch> findByCode(String code);
    boolean existsByCode(String code);
}
