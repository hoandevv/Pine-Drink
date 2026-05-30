package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Address.CreateAddressRequest;
import com.hoandev.pinedrink.entity.dto.request.Address.UpdateCusAddress;
import com.hoandev.pinedrink.entity.dto.response.Address.CustomerAddressResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;


/**
 * Service interface for managing customer addresses.
 */
public interface CusAddressService {
    /**
     * Creates a new customer address.
     *
     * @param request the request containing the address details
     * @return the created customer address
     */
    CustomerAddressResponse create(CreateAddressRequest request);

    /**
     * Updates an existing customer address.
     *
     * @param id the ID of the address to update
     * @param request the request containing the updated address details
     * @return the updated customer address
     */
    CustomerAddressResponse update(String id, UpdateCusAddress request);

    /**
     * Deletes a customer address.
     *
     * @param id the ID of the address to delete
     */
    void delete(String id);

    /**
     * Retrieves a customer address by its ID.
     *
     * @param id the ID of the address to retrieve
     * @return the customer address
     */
    CustomerAddressResponse getById(String id);

    /**
     * Retrieves all addresses for the current authenticated customer.
     *
     * @return list of customer addresses
     */
    PageResponse<CustomerAddressResponse> getAllByCurrentCustomer(Pageable pageable);

    /**
     * Sets an address as default for the current customer.
     *
     * @param id the ID of the address to set as default
     * @return the updated customer address
     */
    CustomerAddressResponse setDefault(String id);
}
