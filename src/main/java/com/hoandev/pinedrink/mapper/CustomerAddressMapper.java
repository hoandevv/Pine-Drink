package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.CustomerAddress;
import com.hoandev.pinedrink.entity.CustomerProfile;
import com.hoandev.pinedrink.entity.dto.request.Address.CreateAddressRequest;
import com.hoandev.pinedrink.entity.dto.request.Address.UpdateCusAddress;
import com.hoandev.pinedrink.entity.dto.response.Address.CustomerAddressResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for CustomerAddress entity and DTOs.
 */
@Component
public class CustomerAddressMapper {

    /**
     * Maps CustomerAddress entity to CustomerAddressResponse DTO.
     *
     * @param address the customer address entity
     * @return the customer address response DTO
     */
    public CustomerAddressResponse toResponse(CustomerAddress address) {
        if (address == null) {
            return null;
        }

        return CustomerAddressResponse.builder()
                .id(address.getId())
                .receiverName(address.getReceiverName())
                .receiverPhone(address.getReceiverPhone())
                .addressLine(address.getAddressLine())
                .ward(address.getWard())
                .district(address.getDistrict())
                .city(address.getCity())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .isDefault(address.isDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }

    /**
     * Maps CreateAddressRequest to CustomerAddress entity.
     *
     * @param request the create address request
     * @param customer the customer profile
     * @return the customer address entity
     */
    public CustomerAddress toEntity(CreateAddressRequest request, CustomerProfile customer) {
        if (request == null) {
            return null;
        }

        CustomerAddress address = new CustomerAddress();
        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setAddressLine(request.getAddressLine());
        address.setWard(request.getWard());
        address.setDistrict(request.getDistrict());
        address.setCity(request.getCity());
        address.setDefault(request.isDefault());
        address.setCustomer(customer);
        return address;
    }

    /**
     * Updates CustomerAddress entity from UpdateCusAddress request.
     *
     * @param address the customer address entity to update
     * @param request the update address request
     */
    public void updateEntity(CustomerAddress address, UpdateCusAddress request) {
        if (address == null || request == null) {
            return;
        }

        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setAddressLine(request.getAddressLine());
        address.setWard(request.getWard());
        address.setDistrict(request.getDistrict());
        address.setCity(request.getCity());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setDefault(request.isDefault());
    }
}
