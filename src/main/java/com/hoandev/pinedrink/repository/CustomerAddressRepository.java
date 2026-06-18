package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.CustomerAddress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, String> {
   /**
     * Finds all customer addresses by customer ID.
     *
     * @param customerId the ID of the customer
     * @return a list of customer addresses
     */
    List<CustomerAddress> findByCustomerId(String customerId);
    Page<CustomerAddress> findByCustomerId(String customerId, Pageable pageable);
    Optional<CustomerAddress> findByCustomerIdAndIsDefaultTrue(String customerId);
}
