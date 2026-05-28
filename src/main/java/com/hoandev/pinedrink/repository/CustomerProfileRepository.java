package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing {@link CustomerProfile} entities.
 */
@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, String> {

    /**
     * Finds a customer profile by the owning account's ID.
     *
     * @param accountId the account ID to search for
     * @return an {@link Optional} containing the profile if found
     */
    Optional<CustomerProfile> findByAccountId(String accountId);

    /**
     * Finds a customer profile by its unique customer code.
     *
     * @param customerCode the customer code to search for
     * @return an {@link Optional} containing the profile if found
     */
    Optional<CustomerProfile> findByCustomerCode(String customerCode);
}
