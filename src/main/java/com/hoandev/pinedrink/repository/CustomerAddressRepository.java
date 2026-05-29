package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, String> {
   /**
     * Finds all customer addresses by customer ID.
     *
     * @param customerId the ID of the customer
     * @return a list of customer addresses
     */
    List<CustomerAddress> findByCustomerId(String customerId);
}
