package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, String> {
    List<Branch> findByBrandId(String brandId);
    List<Branch> findByBrandIdAndStatus(String brandId, String status);
    Optional<Branch> findByCode(String code);
    boolean existsByCode(String code);
}
