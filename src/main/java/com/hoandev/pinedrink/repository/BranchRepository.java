package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface BranchRepository extends JpaRepository<Branch, String> {
    Page<Branch> findByStatus(String status, Pageable pageable);

    List<Branch> findByStatusOrderByCreatedAtDesc(String status);

    Page<Branch> findByIdIn(Set<String> ids, Pageable pageable);

    @Query("""
            SELECT b
            FROM Branch b
            WHERE b.id IN :ids
              AND b.status = :status
            ORDER BY b.createdAt DESC
            """)
    List<Branch> findActiveBranchesByIds(@Param("ids") Set<String> ids, @Param("status") String status);

    Page<Branch> findByIdInAndStatus(Set<String> ids, String status, Pageable pageable);

    boolean existsByCode(String code);
}
