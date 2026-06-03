package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Branch.*;
import com.hoandev.pinedrink.entity.dto.response.Branch.*;

import java.util.List;

public interface BranchAvailabilityService {
    BranchProductAvailabilityResponse createProductAvailability(String branchId, CreateBranchProductAvailabilityRequest request);
    BranchProductAvailabilityResponse updateProductAvailability(String branchId, String id, UpdateBranchProductAvailabilityRequest request);
    void deleteProductAvailability(String branchId, String id);
    BranchProductAvailabilityResponse getProductAvailability(String branchId, String id);
    List<BranchProductAvailabilityResponse> getProductAvailabilities(String branchId);

    BranchToppingAvailabilityResponse createToppingAvailability(String branchId, CreateBranchToppingAvailabilityRequest request);
    BranchToppingAvailabilityResponse updateToppingAvailability(String branchId, String id, UpdateBranchToppingAvailabilityRequest request);
    void deleteToppingAvailability(String branchId, String id);
    BranchToppingAvailabilityResponse getToppingAvailability(String branchId, String id);
    List<BranchToppingAvailabilityResponse> getToppingAvailabilities(String branchId);
}
