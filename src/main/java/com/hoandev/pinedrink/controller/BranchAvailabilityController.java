package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.Branch.*;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.Branch.*;
import com.hoandev.pinedrink.service.BranchAvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/branches/{branchId}/availability")
@RequiredArgsConstructor
@Slf4j
public class BranchAvailabilityController {
    private final BranchAvailabilityService branchAvailabilityService;

    @PostMapping("/products")
    @PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
    public ResponseEntity<BaseResponse<BranchProductAvailabilityResponse>> createProductAvailability(
            @PathVariable String branchId,
            @Valid @RequestBody CreateBranchProductAvailabilityRequest request) {
        log.info("Creating branch product availability: branchId={}", branchId);
        BranchProductAvailabilityResponse response = branchAvailabilityService.createProductAvailability(branchId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Branch product availability created successfully"));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
    public ResponseEntity<BaseResponse<BranchProductAvailabilityResponse>> updateProductAvailability(
            @PathVariable String branchId,
            @PathVariable String id,
            @Valid @RequestBody UpdateBranchProductAvailabilityRequest request) {
        log.info("Updating branch product availability: branchId={}, id={}", branchId, id);
        BranchProductAvailabilityResponse response = branchAvailabilityService.updateProductAvailability(branchId, id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch product availability updated successfully"));
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
    public ResponseEntity<BaseResponse<Void>> deleteProductAvailability(@PathVariable String branchId, @PathVariable String id) {
        log.info("Deleting branch product availability: branchId={}, id={}", branchId, id);
        branchAvailabilityService.deleteProductAvailability(branchId, id);
        return ResponseEntity.ok(BaseResponse.success(null, "Branch product availability deleted successfully"));
    }

    @GetMapping("/products/{id}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_VIEW')")
    public ResponseEntity<BaseResponse<BranchProductAvailabilityResponse>> getProductAvailability(@PathVariable String branchId, @PathVariable String id) {
        log.info("Getting branch product availability: branchId={}, id={}", branchId, id);
        BranchProductAvailabilityResponse response = branchAvailabilityService.getProductAvailability(branchId, id);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch product availability retrieved successfully"));
    }

    @GetMapping("/products")
    @PreAuthorize("hasAuthority('PERM_BRANCH_VIEW')")
    public ResponseEntity<BaseResponse<List<BranchProductAvailabilityResponse>>> getProductAvailabilities(@PathVariable String branchId) {
        log.info("Getting branch product availabilities: branchId={}", branchId);
        List<BranchProductAvailabilityResponse> response = branchAvailabilityService.getProductAvailabilities(branchId);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch product availabilities retrieved successfully"));
    }

    @PostMapping("/toppings")
    @PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
    public ResponseEntity<BaseResponse<BranchToppingAvailabilityResponse>> createToppingAvailability(
            @PathVariable String branchId,
            @Valid @RequestBody CreateBranchToppingAvailabilityRequest request) {
        log.info("Creating branch topping availability: branchId={}", branchId);
        BranchToppingAvailabilityResponse response = branchAvailabilityService.createToppingAvailability(branchId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Branch topping availability created successfully"));
    }

    @PutMapping("/toppings/{id}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
    public ResponseEntity<BaseResponse<BranchToppingAvailabilityResponse>> updateToppingAvailability(
            @PathVariable String branchId,
            @PathVariable String id,
            @Valid @RequestBody UpdateBranchToppingAvailabilityRequest request) {
        log.info("Updating branch topping availability: branchId={}, id={}", branchId, id);
        BranchToppingAvailabilityResponse response = branchAvailabilityService.updateToppingAvailability(branchId, id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch topping availability updated successfully"));
    }

    @DeleteMapping("/toppings/{id}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
    public ResponseEntity<BaseResponse<Void>> deleteToppingAvailability(@PathVariable String branchId, @PathVariable String id) {
        log.info("Deleting branch topping availability: branchId={}, id={}", branchId, id);
        branchAvailabilityService.deleteToppingAvailability(branchId, id);
        return ResponseEntity.ok(BaseResponse.success(null, "Branch topping availability deleted successfully"));
    }

    @GetMapping("/toppings/{id}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_VIEW')")
    public ResponseEntity<BaseResponse<BranchToppingAvailabilityResponse>> getToppingAvailability(@PathVariable String branchId, @PathVariable String id) {
        log.info("Getting branch topping availability: branchId={}, id={}", branchId, id);
        BranchToppingAvailabilityResponse response = branchAvailabilityService.getToppingAvailability(branchId, id);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch topping availability retrieved successfully"));
    }

    @GetMapping("/toppings")
    @PreAuthorize("hasAuthority('PERM_BRANCH_VIEW')")
    public ResponseEntity<BaseResponse<List<BranchToppingAvailabilityResponse>>> getToppingAvailabilities(@PathVariable String branchId) {
        log.info("Getting branch topping availabilities: branchId={}", branchId);
        List<BranchToppingAvailabilityResponse> response = branchAvailabilityService.getToppingAvailabilities(branchId);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch topping availabilities retrieved successfully"));
    }

}
