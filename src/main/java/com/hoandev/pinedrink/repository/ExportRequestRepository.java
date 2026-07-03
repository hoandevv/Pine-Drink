package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.ExportRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
    /**
     * Lấy ra tất cả các yêu cầu xuất dữ liệu của người dùng dựa trên ID của họ, với phân trang.
     *
     * @param requestedById The ID of the user who requested the exports.
     * @param pageable      The pagination information.
     * @return A page of export requests.
     */
    Page<ExportRequest> findByRequestedById(String requestedById, Pageable pageable);
}
