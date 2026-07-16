package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.Voucher.CreateVoucherRequest;
import com.hoandev.pinedrink.entity.dto.request.Voucher.UpdateVoucherRequest;
import com.hoandev.pinedrink.entity.dto.request.Voucher.UpdateVoucherStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Voucher.VoucherResponse;
import com.hoandev.pinedrink.entity.dto.response.Voucher.VoucherSummaryResponse;
import com.hoandev.pinedrink.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
@Slf4j
public class VoucherController {

    private final VoucherService voucherService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_VOUCHER_CREATE')")
    public ResponseEntity<BaseResponse<VoucherResponse>> create(@Valid @RequestBody CreateVoucherRequest request) {
        log.info("Creating voucher: code={}", request.getCode());
        VoucherResponse response = voucherService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Voucher created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_VOUCHER_UPDATE')")
    public ResponseEntity<BaseResponse<VoucherResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateVoucherRequest request) {
        log.info("Updating voucher: id={}", id);
        VoucherResponse response = voucherService.update(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Voucher updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERM_VOUCHER_UPDATE')")
    public ResponseEntity<BaseResponse<VoucherResponse>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateVoucherStatusRequest request) {
        log.info("Updating voucher status: id={}, status={}", id, request.getStatus());
        VoucherResponse response = voucherService.updateStatus(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Voucher status updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_VOUCHER_DELETE')")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable String id) {
        log.info("Deleting voucher: id={}", id);
        voucherService.delete(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Voucher deleted successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_VOUCHER_VIEW')")
    public ResponseEntity<BaseResponse<VoucherResponse>> getById(@PathVariable String id) {
        log.info("Getting voucher: id={}", id);
        VoucherResponse response = voucherService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Voucher retrieved successfully"));
    }

    @GetMapping({"", "/summaries"})
    @PreAuthorize("hasAuthority('PERM_VOUCHER_VIEW')")
    public ResponseEntity<BaseResponse<PageResponse<VoucherSummaryResponse>>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String discountType,
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime activeAt,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Getting vouchers: status={}, discountType={}, branchId={}", status, discountType, branchId);
        PageResponse<VoucherSummaryResponse> response = voucherService.getSummaries(keyword, status, discountType, branchId, activeAt, pageable);
        return ResponseEntity.ok(BaseResponse.success(response, "Vouchers retrieved successfully"));
    }

    @GetMapping("/customer/available/summaries")
    public ResponseEntity<BaseResponse<PageResponse<VoucherSummaryResponse>>> getAvailableSummariesForCustomer(
            @RequestParam String branchId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Getting available voucher summaries for customer: branchId={}", branchId);
        PageResponse<VoucherSummaryResponse> response = voucherService.getAvailableSummariesForCustomer(branchId, pageable);
        return ResponseEntity.ok(BaseResponse.success(response, "Available voucher summaries retrieved successfully"));
    }

    @GetMapping("/customer/available")
    public ResponseEntity<BaseResponse<PageResponse<VoucherSummaryResponse>>> getAvailableForCustomer(
            @RequestParam String branchId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Getting available vouchers for customer: branchId={}", branchId);
        PageResponse<VoucherSummaryResponse> response = voucherService.getAvailableForCustomer(branchId, pageable);
        return ResponseEntity.ok(BaseResponse.success(response, "Available vouchers retrieved successfully"));
    }
}
