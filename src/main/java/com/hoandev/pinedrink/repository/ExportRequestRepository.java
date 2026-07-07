package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.ExportRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ExportRequestRepository extends JpaRepository<ExportRequest, String> {

    @Query("""
            SELECT e
            FROM ExportRequest e
            LEFT JOIN FETCH e.requestedBy
            LEFT JOIN FETCH e.branch
            WHERE e.id = :id
            """)
    Optional<ExportRequest> findByIdWithRequestedBy(@Param("id") String id);

    Page<ExportRequest> findByRequestedById(String requestedById, Pageable pageable);

    @Query("""
            SELECT COUNT(e)
            FROM ExportRequest e
            WHERE e.requestedBy.id = :requestedById
            """)
    long countByRequestedById(@Param("requestedById") String requestedById);

    @Query("""
            SELECT COUNT(e)
            FROM ExportRequest e
            WHERE e.requestedBy.id = :requestedById
              AND e.status = :status
            """)
    long countByStatus(@Param("requestedById") String requestedById,
                       @Param("status") String status);

    @Query("""
            SELECT COUNT(e)
            FROM ExportRequest e
            WHERE e.requestedBy.id = :requestedById
              AND e.status = :status
              AND e.startedAt < :startedAt
            """)
    long countStaleByStatus(@Param("requestedById") String requestedById,
                            @Param("status") String status,
                            @Param("startedAt") LocalDateTime startedAt);
}
