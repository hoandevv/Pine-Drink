package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.*;
import com.hoandev.pinedrink.entity.dto.request.Branch.*;
import com.hoandev.pinedrink.entity.dto.response.Branch.*;
import org.springframework.stereotype.Component;

@Component
public class BranchAvailabilityMapper {

    public BranchProductAvailabilityResponse toProductResponse(BranchProductAvailability availability) {
        if (availability == null) {
            return null;
        }

        return BranchProductAvailabilityResponse.builder()
                .id(availability.getId())
                .branchId(availability.getBranch().getId())
                .productId(availability.getProduct().getId())
                .productName(availability.getProduct().getName())
                .available(availability.isAvailable())
                .salePrice(availability.getSalePrice())
                .soldOutReason(availability.getSoldOutReason())
                .availableFrom(availability.getAvailableFrom())
                .availableTo(availability.getAvailableTo())
                .status(availability.getStatus())
                .createdAt(availability.getCreatedAt())
                .updatedAt(availability.getUpdatedAt())
                .build();
    }

    public BranchProductAvailability toProductEntity(CreateBranchProductAvailabilityRequest request, Branch branch, Product product) {
        BranchProductAvailability availability = new BranchProductAvailability();
        availability.setBranch(branch);
        availability.setProduct(product);
        availability.setAvailable(request.isAvailable());
        availability.setSalePrice(request.getSalePrice());
        availability.setSoldOutReason(request.getSoldOutReason());
        availability.setAvailableFrom(request.getAvailableFrom());
        availability.setAvailableTo(request.getAvailableTo());
        return availability;
    }

    public void updateProductEntity(BranchProductAvailability availability, UpdateBranchProductAvailabilityRequest request, Product product) {
        if (request.getAvailable() != null) {
            availability.setAvailable(request.getAvailable());
        }
        if (request.getSalePrice() != null) {
            availability.setSalePrice(request.getSalePrice());
        }
        if (request.getSoldOutReason() != null) {
            availability.setSoldOutReason(request.getSoldOutReason());
        }
        if (request.getAvailableFrom() != null) {
            availability.setAvailableFrom(request.getAvailableFrom());
        }
        if (request.getAvailableTo() != null) {
            availability.setAvailableTo(request.getAvailableTo());
        }
        if (product != null) {
            availability.setProduct(product);
        }
    }

    public BranchToppingAvailabilityResponse toToppingResponse(BranchToppingAvailability availability) {
        if (availability == null) {
            return null;
        }

        return BranchToppingAvailabilityResponse.builder()
                .id(availability.getId())
                .branchId(availability.getBranch().getId())
                .toppingId(availability.getTopping().getId())
                .toppingName(availability.getTopping().getName())
                .available(availability.isAvailable())
                .soldOutReason(availability.getSoldOutReason())
                .status(availability.getStatus())
                .createdAt(availability.getCreatedAt())
                .updatedAt(availability.getUpdatedAt())
                .build();
    }

    public BranchToppingAvailability toToppingEntity(CreateBranchToppingAvailabilityRequest request, Branch branch, Topping topping) {
        BranchToppingAvailability availability = new BranchToppingAvailability();
        availability.setBranch(branch);
        availability.setTopping(topping);
        availability.setAvailable(request.isAvailable());
        availability.setSoldOutReason(request.getSoldOutReason());
        return availability;
    }

    public void updateToppingEntity(BranchToppingAvailability availability, UpdateBranchToppingAvailabilityRequest request, Topping topping) {
        if (request.getAvailable() != null) {
            availability.setAvailable(request.getAvailable());
        }
        if (request.getSoldOutReason() != null) {
            availability.setSoldOutReason(request.getSoldOutReason());
        }
        if (topping != null) {
            availability.setTopping(topping);
        }
    }
}
