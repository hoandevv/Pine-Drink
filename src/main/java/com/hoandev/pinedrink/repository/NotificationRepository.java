package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByRecipientAccountIdAndStatusOrderByCreatedAtDesc(String recipientAccountId, String status);
    List<Notification> findByBranchIdOrderByCreatedAtDesc(String branchId);
}
