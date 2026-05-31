package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.Branch.CreateBranchRequest;
import com.hoandev.pinedrink.entity.dto.request.Branch.UpdateBranchStatusRequest;
import com.hoandev.pinedrink.entity.dto.request.Branch.UpdateBranchRequest;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.Branch.BranchResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


/**
 * REST controller for managing branches.
 * Requires branch permissions for back-office operations.
 */
@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
@Slf4j
public class BranchController {
    private final BranchService branchService;

    /**
     * Creates a new branch.
     *
     * @param request the create branch request
     * @return the created branch
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_BRANCH_CREATE')")
    public ResponseEntity<BaseResponse<BranchResponse>> create(@Valid @RequestBody CreateBranchRequest request) {
        log.info("Creating branch for brandId={}", request.getBrandId());
        BranchResponse response = branchService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Branch created successfully"));
    }

    /**
     * Updates an existing branch.
     *
     * @param id the branch ID
     * @param request the update branch request
     * @return the updated branch
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
    public ResponseEntity<BaseResponse<BranchResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateBranchRequest request) {
        log.info("Updating branch: id={}", id);
        BranchResponse response = branchService.update(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch updated successfully"));
    }

    /**
     * Updates branch status.
     *
     * @param id the branch ID
     * @param request the status update request
     * @return the updated branch
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
    public ResponseEntity<BaseResponse<BranchResponse>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateBranchStatusRequest request) {
        log.info("Updating branch status: id={}, status={}", id, request.getStatus());
        BranchResponse response = branchService.updateStatus(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch status updated successfully"));
    }

    /**
     * Deletes a branch (soft delete).
     *
     * @param id the branch ID
     * @return success response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_DELETE')")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable String id) {
        log.info("Deleting branch: id={}", id);
        branchService.delete(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Branch deleted successfully"));
    }

    /**
     * Retrieves a branch by ID.
     *
     * @param id the branch ID
     * @return the branch
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_VIEW')")
    public ResponseEntity<BaseResponse<BranchResponse>> getById(@PathVariable String id) {
        log.info("Getting branch: id={}", id);
        BranchResponse response = branchService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch retrieved successfully"));
    }

    /**
     * Retrieves all branches for a specific brand.
     *
     * @param brandId the brand ID
     * @return list of branches
     */
    @GetMapping("/brand/{brandId}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_VIEW')")
    public ResponseEntity<BaseResponse<PageResponse<BranchResponse>>> getAllByBrandId(
            @PathVariable String brandId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Getting all branches for brand: brandId={}", brandId);
        PageResponse<BranchResponse> responses = branchService.getAllByBrandId(brandId, pageable);
        return ResponseEntity.ok(BaseResponse.success(responses, "Branches retrieved successfully"));
    }

    /**
     * Retrieves all active branches for a specific brand.
     *
     * @param brandId the brand ID
     * @return list of active branches
     */
    @GetMapping("/brand/{brandId}/active")
    @PreAuthorize("hasAuthority('PERM_BRANCH_VIEW')")
    public ResponseEntity<BaseResponse<PageResponse<BranchResponse>>> getAllActiveByBrandId(
            @PathVariable String brandId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Getting all active branches for brand: brandId={}", brandId);
        PageResponse<BranchResponse> responses = branchService.getAllActiveByBrandId(brandId, pageable);
        return ResponseEntity.ok(BaseResponse.success(responses, "Active branches retrieved successfully"));
    }
}
