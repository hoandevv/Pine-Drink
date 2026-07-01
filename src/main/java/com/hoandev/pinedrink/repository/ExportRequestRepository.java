package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.ExportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExportRequestRepository extends JpaRepository<ExportRequest, String> {
    List<ExportRequest> findByRequestedByIdOrderByCreatedAtDesc(String requestedById);

    @Query("""
            SELECT e
            FROM ExportRequest e
            LEFT JOIN FETCH e.requestedBy
            LEFT JOIN FETCH e.branch
            WHERE e.id = :id
            """)
    Optional<ExportRequest> findByIdWithRequestedBy(@Param("id") String id);
}
