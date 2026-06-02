package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.Branch.CreateBranchHoursRequest;
import com.hoandev.pinedrink.entity.dto.request.Branch.UpdateBranchHoursRequest;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.Branch.BranchHoursResponse;
import com.hoandev.pinedrink.service.BranchHoursService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branches/{branchId}/hours")
@RequiredArgsConstructor
@Slf4j
public class BranchHoursController {
    private final BranchHoursService branchHoursService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
    public ResponseEntity<BaseResponse<BranchHoursResponse>> create(
            @PathVariable String branchId,
            @Valid @RequestBody CreateBranchHoursRequest request) {
        log.info("Creating branch hours: branchId={}", branchId);
        BranchHoursResponse response = branchHoursService.create(branchId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Branch hours created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
    public ResponseEntity<BaseResponse<BranchHoursResponse>> update(
            @PathVariable String branchId,
            @PathVariable String id,
            @Valid @RequestBody UpdateBranchHoursRequest request) {
        log.info("Updating branch hours: branchId={}, id={}", branchId, id);
        BranchHoursResponse response = branchHoursService.update(branchId, id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch hours updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable String branchId, @PathVariable String id) {
        log.info("Deleting branch hours: branchId={}, id={}", branchId, id);
        branchHoursService.delete(branchId, id);
        return ResponseEntity.ok(BaseResponse.success(null, "Branch hours deleted successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_VIEW')")
    public ResponseEntity<BaseResponse<BranchHoursResponse>> getById(@PathVariable String branchId, @PathVariable String id) {
        log.info("Getting branch hours: branchId={}, id={}", branchId, id);
        BranchHoursResponse response = branchHoursService.getById(branchId, id);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch hours retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_BRANCH_VIEW')")
    public ResponseEntity<BaseResponse<List<BranchHoursResponse>>> getByBranch(@PathVariable String branchId) {
        log.info("Getting branch hours: branchId={}", branchId);
        List<BranchHoursResponse> response = branchHoursService.getByBranch(branchId);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch hours retrieved successfully"));
    }
}
