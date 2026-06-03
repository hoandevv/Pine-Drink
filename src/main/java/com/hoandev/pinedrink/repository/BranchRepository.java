package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepository extends JpaRepository<Branch, String> {
    Page<Branch> findByStatus(String status, Pageable pageable);

    Page<Branch> findByIdIn(java.util.Set<String> ids, Pageable pageable);

    Page<Branch> findByIdInAndStatus(java.util.Set<String> ids, String status, Pageable pageable);

    boolean existsByCode(String code);
}
