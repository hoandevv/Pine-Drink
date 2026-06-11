package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.DailyStock.SetDailyStockQuotaRequest;
import com.hoandev.pinedrink.entity.dto.request.DailyStock.CopyDailyStockQuotaRequest;
import com.hoandev.pinedrink.entity.dto.request.DailyStock.UpdateDailyStockQuotaRequest;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.DailyStock.CopyDailyStockQuotaResponse;
import com.hoandev.pinedrink.entity.dto.response.DailyStock.DailyStockLogResponse;
import com.hoandev.pinedrink.entity.dto.response.DailyStock.DailyStockResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.service.BranchVariantDailyStockService;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/daily-stocks")
@RequiredArgsConstructor
@Slf4j
public class DailyStockController {
    private final BranchVariantDailyStockService dailyStockService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_PRODUCT_VIEW')")
    public ResponseEntity<BaseResponse<List<DailyStockResponse>>> getByBranchAndDate(
            @RequestParam String branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate stockDate = date == null ? LocalDate.now() : date;
        return ResponseEntity.ok(BaseResponse.success(
                dailyStockService.getByBranchAndDate(branchId, stockDate),
                "Daily stocks retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_PRODUCT_UPDATE')")
    public ResponseEntity<BaseResponse<DailyStockResponse>> setQuota(@Valid @RequestBody SetDailyStockQuotaRequest request) {
        DailyStockResponse response = dailyStockService.setQuota(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(response, "Daily stock quota saved successfully"));
    }

    @PatchMapping("/{id}/quota")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_UPDATE')")
    public ResponseEntity<BaseResponse<DailyStockResponse>> updateQuota(
            @PathVariable String id,
            @Valid @RequestBody UpdateDailyStockQuotaRequest request) {
        return ResponseEntity.ok(BaseResponse.success(dailyStockService.updateQuota(id, request), "Daily stock quota updated successfully"));
    }

    @PostMapping("/copy")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_UPDATE')")
    public ResponseEntity<BaseResponse<CopyDailyStockQuotaResponse>> copyQuota(@Valid @RequestBody CopyDailyStockQuotaRequest request) {
        return ResponseEntity.ok(BaseResponse.success(dailyStockService.copyQuota(request), "Daily stock quota copied successfully"));
    }

    @GetMapping("/{id}/logs")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_VIEW')")
    public ResponseEntity<BaseResponse<PageResponse<DailyStockLogResponse>>> getLogs(
            @PathVariable String id,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(BaseResponse.success(dailyStockService.getLogs(id, pageable), "Daily stock logs retrieved successfully"));
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_VIEW')")
    public ResponseEntity<BaseResponse<PageResponse<DailyStockLogResponse>>> getLogsByOrder(
            @RequestParam String orderId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(BaseResponse.success(dailyStockService.getLogsByOrder(orderId, pageable), "Daily stock logs retrieved successfully"));
    }
}
