package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface BranchRepository extends JpaRepository<Branch, String> {
    Page<Branch> findByStatus(String status, Pageable pageable);

    Page<Branch> findByIdIn(Set<String> ids, Pageable pageable);

    Page<Branch> findByIdInAndStatus(Set<String> ids, String status, Pageable pageable);

    boolean existsByCode(String code);
}
