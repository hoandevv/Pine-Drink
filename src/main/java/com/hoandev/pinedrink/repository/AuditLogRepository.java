package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    List<AuditLog> findByActorAccountIdOrderByCreatedAtDesc(String actorAccountId);
}
