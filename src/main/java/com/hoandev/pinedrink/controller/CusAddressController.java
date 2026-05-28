package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.Address.CreateAddressRequest;
import com.hoandev.pinedrink.entity.dto.request.Address.UpdateCusAddress;
import com.hoandev.pinedrink.entity.dto.response.Address.CustomerAddressResponse;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.service.CusAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing customer addresses.
 * All endpoints require CUSTOMER role authentication.
 */
@RestController
@RequestMapping("/api/v1/customer/addresses")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('CUSTOMER')")
public class CusAddressController {
    private final CusAddressService cusAddressService;

    /**
     * Creates a new customer address.
     *
     * @param request the create address request
     * @return the created customer address
     */
    @PostMapping
    public ResponseEntity<BaseResponse<CustomerAddressResponse>> create(@Valid @RequestBody CreateAddressRequest request) {
        log.info("Creating customer address");
        CustomerAddressResponse response = cusAddressService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Customer address created successfully"));
    }

    /**
     * Updates an existing customer address.
     *
     * @param id the address ID
     * @param request the update address request
     * @return the updated customer address
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<CustomerAddressResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateCusAddress request) {
        log.info("Updating customer address: id={}", id);
        CustomerAddressResponse response = cusAddressService.update(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Customer address updated successfully"));
    }

    /**
     * Deletes a customer address.
     *
     * @param id the address ID
     * @return success response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable String id) {
        log.info("Deleting customer address: id={}", id);
        cusAddressService.delete(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Customer address deleted successfully"));
    }

    /**
     * Retrieves a customer address by ID.
     *
     * @param id the address ID
     * @return the customer address
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<CustomerAddressResponse>> getById(@PathVariable String id) {
        log.info("Getting customer address: id={}", id);
        CustomerAddressResponse response = cusAddressService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Customer address retrieved successfully"));
    }

    /**
     * Retrieves all addresses for the current authenticated customer.
     *
     * @return list of customer addresses
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<CustomerAddressResponse>>> getAllByCurrentCustomer() {
        log.info("Getting all customer addresses for current customer");
        List<CustomerAddressResponse> responses = cusAddressService.getAllByCurrentCustomer();
        return ResponseEntity.ok(BaseResponse.success(responses, "Customer addresses retrieved successfully"));
    }

    /**
     * Sets an address as default for the current customer.
     *
     * @param id the address ID
     * @return the updated customer address
     */
    @PatchMapping("/{id}/set-default")
    public ResponseEntity<BaseResponse<CustomerAddressResponse>> setDefault(@PathVariable String id) {
        log.info("Setting customer address as default: id={}", id);
        CustomerAddressResponse response = cusAddressService.setDefault(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Customer address set as default successfully"));
    }
}
