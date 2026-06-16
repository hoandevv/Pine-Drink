package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.DailyStock.SetDailyStockQuotaRequest;
import com.hoandev.pinedrink.entity.dto.request.DailyStock.CopyDailyStockQuotaRequest;
import com.hoandev.pinedrink.entity.dto.request.DailyStock.UpdateDailyStockQuotaRequest;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.DailyStock.CopyDailyStockQuotaResponse;
import com.hoandev.pinedrink.entity.dto.response.DailyStock.DailyStockLogResponse;
import com.hoandev.pinedrink.entity.dto.response.DailyStock.DailyStockResponse;
import com.hoandev.pinedrink.entity.dto.response.DailyStock.PublicDailyStockResponse;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class DailyStockController {
    private final BranchVariantDailyStockService dailyStockService;

    @GetMapping("/admin/daily-stocks")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_VIEW')")
    public ResponseEntity<BaseResponse<List<DailyStockResponse>>> getByBranchAndDate(
            @RequestParam String branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate stockDate = date == null ? LocalDate.now() : date;
        return ResponseEntity.ok(BaseResponse.success(
                dailyStockService.getByBranchAndDate(branchId, stockDate),
                "Daily stocks retrieved successfully"));
    }

    @GetMapping("/branches/{branchId}/daily-stocks")
    public ResponseEntity<BaseResponse<List<PublicDailyStockResponse>>> getPublicByBranchAndDate(
            @PathVariable String branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate stockDate = date == null ? LocalDate.now() : date;
        List<PublicDailyStockResponse> response = dailyStockService.getByBranchAndDate(branchId, stockDate)
                .stream()
                .map(this::toPublicResponse)
                .toList();
        return ResponseEntity.ok(BaseResponse.success(response, "Daily stock availability retrieved successfully"));
    }

    @PostMapping("/admin/daily-stocks")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_UPDATE')")
    public ResponseEntity<BaseResponse<DailyStockResponse>> setQuota(@Valid @RequestBody SetDailyStockQuotaRequest request) {
        DailyStockResponse response = dailyStockService.setQuota(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(response, "Daily stock quota saved successfully"));
    }

    @PatchMapping("/admin/daily-stocks/{id}/quota")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_UPDATE')")
    public ResponseEntity<BaseResponse<DailyStockResponse>> updateQuota(
            @PathVariable String id,
            @Valid @RequestBody UpdateDailyStockQuotaRequest request) {
        return ResponseEntity.ok(BaseResponse.success(dailyStockService.updateQuota(id, request), "Daily stock quota updated successfully"));
    }

    @PostMapping("/admin/daily-stocks/copy")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_UPDATE')")
    public ResponseEntity<BaseResponse<CopyDailyStockQuotaResponse>> copyQuota(@Valid @RequestBody CopyDailyStockQuotaRequest request) {
        return ResponseEntity.ok(BaseResponse.success(dailyStockService.copyQuota(request), "Daily stock quota copied successfully"));
    }

    @GetMapping("/admin/daily-stocks/{id}/logs")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_VIEW')")
    public ResponseEntity<BaseResponse<PageResponse<DailyStockLogResponse>>> getLogs(
            @PathVariable String id,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(BaseResponse.success(dailyStockService.getLogs(id, pageable), "Daily stock logs retrieved successfully"));
    }

    @GetMapping("/admin/daily-stocks/logs")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_VIEW')")
    public ResponseEntity<BaseResponse<PageResponse<DailyStockLogResponse>>> getLogsByOrder(
            @RequestParam String orderId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(BaseResponse.success(dailyStockService.getLogsByOrder(orderId, pageable), "Daily stock logs retrieved successfully"));
    }

    private PublicDailyStockResponse toPublicResponse(DailyStockResponse stock) {
        return PublicDailyStockResponse.builder()
                .branchId(stock.getBranchId())
                .productId(stock.getProductId())
                .productName(stock.getProductName())
                .variantId(stock.getVariantId())
                .variantName(stock.getVariantName())
                .stockDate(stock.getStockDate())
                .availableQuantity(stock.getAvailableQuantity())
                .stockStatus(stock.getStockStatus())
                .status(stock.getStatus())
                .build();
    }
}
