package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.LoyaltyPointHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoyaltyPointHistoryRepository extends JpaRepository<LoyaltyPointHistory, String> {
    List<LoyaltyPointHistory> findByLoyaltyAccountIdOrderByCreatedAtDesc(String loyaltyAccountId);
}
