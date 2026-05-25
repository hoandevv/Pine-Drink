package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.ExportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExportRequestRepository extends JpaRepository<ExportRequest, String> {
    List<ExportRequest> findByRequestedByOrderByRequestedAtDesc(String requestedBy);
}
