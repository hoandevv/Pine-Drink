package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.CustomerAddress;
import com.hoandev.pinedrink.entity.CustomerProfile;
import com.hoandev.pinedrink.entity.dto.request.Address.CreateAddressRequest;
import com.hoandev.pinedrink.entity.dto.request.Address.UpdateCusAddress;
import com.hoandev.pinedrink.entity.dto.response.Address.CustomerAddressResponse;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.CustomerAddressMapper;
import com.hoandev.pinedrink.queue.event.geocoding.GeocodingRequestEvent;
import com.hoandev.pinedrink.queue.publisher.EventPublisher;
import com.hoandev.pinedrink.repository.CustomerAddressRepository;
import com.hoandev.pinedrink.repository.CustomerProfileRepository;
import com.hoandev.pinedrink.security.UserPrincipal;
import com.hoandev.pinedrink.service.CusAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CusAddressServiceImpl implements CusAddressService {
    private final CustomerAddressRepository customerAddressRepository;
    private final CustomerAddressMapper customerAddressMapper;
    private final CustomerProfileRepository customerProfileRepository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public CustomerAddressResponse create(CreateAddressRequest request) {
        CustomerProfile customer = getCurrentCustomerProfile();

        // If this is set as default, unset other default addresses
        if (request.isDefault()) {
            unsetDefaultAddresses(customer.getId());
        }

        CustomerAddress address = customerAddressMapper.toEntity(request, customer);
        
        // Check if FE already provided lat/lng from map picker
        if (request.getLatitude() != null && request.getLongitude() != null) {
            // Flow 2: User selected from map, coordinates already provided
            address.setLatitude(request.getLatitude());
            address.setLongitude(request.getLongitude());
            address = customerAddressRepository.save(address);
            log.info("Customer address created with coordinates from map picker: id={}, lat={}, lng={}", 
                    address.getId(), request.getLatitude(), request.getLongitude());
        } else {
            // Flow 1: User entered text only, need async geocoding
            address = customerAddressRepository.save(address);
            log.info("Customer address created, geocoding will be processed asynchronously: id={}", address.getId());
            
            // Publish geocoding event asynchronously after transaction commits
            String finalAddressId = address.getId();
            String fullAddress = buildFullAddress(address);
            publishGeocodingEventAfterCommit(finalAddressId, fullAddress);
        }

        return customerAddressMapper.toResponse(address);
    }

    @Override
    @Transactional
    public CustomerAddressResponse update(String id, UpdateCusAddress request) {
        CustomerProfile customer = getCurrentCustomerProfile();
        CustomerAddress address = customerAddressRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.CUSTOMER_002));

        // Verify address belongs to current customer
        if (!address.getCustomer().getId().equals(customer.getId())) {
            throw new BaseException(ErrorCode.CUSTOMER_003);
        }

        // If this is set as default, unset other default addresses
        if (request.isDefault() && !address.isDefault()) {
            unsetDefaultAddresses(customer.getId());
        }

        customerAddressMapper.updateEntity(address, request);
        address = customerAddressRepository.save(address);

        log.info("Customer address updated: id={}, customerId={}", address.getId(), customer.getId());
        return customerAddressMapper.toResponse(address);
    }

    @Override
    @Transactional
    public void delete(String id) {
        CustomerProfile customer = getCurrentCustomerProfile();
        CustomerAddress address = customerAddressRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.CUSTOMER_002));

        // Verify address belongs to current customer
        if (!address.getCustomer().getId().equals(customer.getId())) {
            throw new BaseException(ErrorCode.CUSTOMER_003);
        }

        customerAddressRepository.delete(address);
        log.info("Customer address deleted: id={}, customerId={}", id, customer.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerAddressResponse getById(String id) {
        CustomerProfile customer = getCurrentCustomerProfile();
        CustomerAddress address = customerAddressRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.CUSTOMER_002));

        // Verify address belongs to current customer
        if (!address.getCustomer().getId().equals(customer.getId())) {
            throw new BaseException(ErrorCode.CUSTOMER_003);
        }

        return customerAddressMapper.toResponse(address);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerAddressResponse> getAllByCurrentCustomer() {
        CustomerProfile customer = getCurrentCustomerProfile();
        List<CustomerAddress> addresses = customerAddressRepository.findByCustomerId(customer.getId());

        return addresses.stream()
                .map(customerAddressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomerAddressResponse setDefault(String id) {
        CustomerProfile customer = getCurrentCustomerProfile();
        CustomerAddress address = customerAddressRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.CUSTOMER_002));

        // Verify address belongs to current customer
        if (!address.getCustomer().getId().equals(customer.getId())) {
            throw new BaseException(ErrorCode.CUSTOMER_003);
        }

        // Unset other default addresses
        unsetDefaultAddresses(customer.getId());

        // Set this address as default
        address.setDefault(true);
        address = customerAddressRepository.save(address);

        log.info("Customer address set as default: id={}, customerId={}", address.getId(), customer.getId());
        return customerAddressMapper.toResponse(address);
    }

    /**
     * Gets the current authenticated customer profile.
     *
     * @return the customer profile
     */
    private CustomerProfile getCurrentCustomerProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new BaseException(ErrorCode.AUTH_003);
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return customerProfileRepository.findByAccountId(principal.getId())
                .orElseThrow(() -> new BaseException(ErrorCode.CUSTOMER_001));
    }

    /**
     * Unsets all default addresses for a customer.
     *
     * @param customerId the customer ID
     */
    private void unsetDefaultAddresses(String customerId) {
        List<CustomerAddress> defaultAddresses = customerAddressRepository.findByCustomerId(customerId)
                .stream()
                .filter(CustomerAddress::isDefault)
                .collect(Collectors.toList());

        defaultAddresses.forEach(addr -> addr.setDefault(false));
        customerAddressRepository.saveAll(defaultAddresses);
    }

    /**
     * Builds a full address string for geocoding.
     *
     * @param address the customer address
     * @return the full address string
     */
    private String buildFullAddress(CustomerAddress address) {
        StringBuilder sb = new StringBuilder();
        
        if (address.getAddressLine() != null) {
            sb.append(address.getAddressLine());
        }
        
        if (address.getWard() != null) {
            sb.append(", ").append(address.getWard());
        }
        
        if (address.getDistrict() != null) {
            sb.append(", ").append(address.getDistrict());
        }
        
        if (address.getCity() != null) {
            sb.append(", ").append(address.getCity());
        }
        
        return sb.toString();
    }

    /**
     * Publishes a geocoding event after the current transaction commits.
     *
     * @param addressId the address ID
     * @param fullAddress the full address string
     */
    private void publishGeocodingEventAfterCommit(String addressId, String fullAddress) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            GeocodingRequestEvent event = GeocodingRequestEvent.of(addressId, fullAddress);
                            eventPublisher.publish(event);
                            log.info("Published geocoding event for addressId: {}", addressId);
                        }
                    });
        } else {
            GeocodingRequestEvent event = GeocodingRequestEvent.of(addressId, fullAddress);
            eventPublisher.publish(event);
            log.info("Published geocoding event for addressId: {}", addressId);
        }
    }
}
