package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, String> {
    Optional<CustomerProfile> findByAccountId(String accountId);
    Optional<CustomerProfile> findByCustomerCode(String customerCode);
}
