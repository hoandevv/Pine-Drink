package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.Branch.CreateBranchRequest;
import com.hoandev.pinedrink.entity.dto.request.Branch.UpdateBranchRequest;
import com.hoandev.pinedrink.entity.dto.request.Branch.UpdateBranchStatusRequest;
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

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
@Slf4j
public class BranchController {
    private final BranchService branchService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_BRANCH_CREATE')")
    public ResponseEntity<BaseResponse<BranchResponse>> create(@Valid @RequestBody CreateBranchRequest request) {
        log.info("Creating branch");
        BranchResponse response = branchService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Branch created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
    public ResponseEntity<BaseResponse<BranchResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateBranchRequest request) {
        log.info("Updating branch: id={}", id);
        BranchResponse response = branchService.update(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
    public ResponseEntity<BaseResponse<BranchResponse>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateBranchStatusRequest request) {
        log.info("Updating branch status: id={}, status={}", id, request.getStatus());
        BranchResponse response = branchService.updateStatus(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch status updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_DELETE')")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable String id) {
        log.info("Deleting branch: id={}", id);
        branchService.delete(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Branch deleted successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_VIEW')")
    public ResponseEntity<BaseResponse<BranchResponse>> getById(@PathVariable String id) {
        log.info("Getting branch: id={}", id);
        BranchResponse response = branchService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_BRANCH_VIEW')")
    public ResponseEntity<BaseResponse<PageResponse<BranchResponse>>> getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Getting branches");
        PageResponse<BranchResponse> response = branchService.getAll(pageable);
        return ResponseEntity.ok(BaseResponse.success(response, "Branches retrieved successfully"));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('PERM_BRANCH_VIEW')")
    public ResponseEntity<BaseResponse<PageResponse<BranchResponse>>> getAllActive(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Getting active branches");
        PageResponse<BranchResponse> response = branchService.getAllActive(pageable);
        return ResponseEntity.ok(BaseResponse.success(response, "Active branches retrieved successfully"));
    }
}
