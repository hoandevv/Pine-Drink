package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.ProductTopping.CreateProductVariantRequest;
import com.hoandev.pinedrink.entity.dto.request.ProductTopping.UpdateProductVariantRequest;
import com.hoandev.pinedrink.entity.dto.request.ProductTopping.UpdateProductVariantStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductVariantResponse;
import com.hoandev.pinedrink.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products/{productId}/variants")
@RequiredArgsConstructor
@Slf4j
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_PRODUCT_CREATE')")
    public ResponseEntity<BaseResponse<ProductVariantResponse>> create(
            @PathVariable String productId,
            @Valid @RequestBody CreateProductVariantRequest request) {
        log.info("Creating product variant for productId={}", productId);
        ProductVariantResponse response = productVariantService.create(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Product variant created successfully"));
    }

    @PutMapping("/{variantId}")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_UPDATE')")
    public ResponseEntity<BaseResponse<ProductVariantResponse>> update(
            @PathVariable String productId,
            @PathVariable String variantId,
            @Valid @RequestBody UpdateProductVariantRequest request) {
        log.info("Updating product variant: productId={}, variantId={}", productId, variantId);
        ProductVariantResponse response = productVariantService.update(productId, variantId, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Product variant updated successfully"));
    }

    @PatchMapping("/{variantId}/status")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_UPDATE')")
    public ResponseEntity<BaseResponse<ProductVariantResponse>> updateStatus(
            @PathVariable String productId,
            @PathVariable String variantId,
            @Valid @RequestBody UpdateProductVariantStatusRequest request) {
        log.info("Updating product variant status: productId={}, variantId={}, status={}", productId, variantId, request.getStatus());
        ProductVariantResponse response = productVariantService.updateStatus(productId, variantId, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Product variant status updated successfully"));
    }

    @DeleteMapping("/{variantId}")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_DELETE')")
    public ResponseEntity<BaseResponse<Void>> delete(
            @PathVariable String productId,
            @PathVariable String variantId) {
        log.info("Deleting product variant: productId={}, variantId={}", productId, variantId);
        productVariantService.delete(productId, variantId);
        return ResponseEntity.ok(BaseResponse.success(null, "Product variant deleted successfully"));
    }

    @GetMapping("/{variantId}")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_VIEW')")
    public ResponseEntity<BaseResponse<ProductVariantResponse>> getById(
            @PathVariable String productId,
            @PathVariable String variantId) {
        log.info("Getting product variant: productId={}, variantId={}", productId, variantId);
        ProductVariantResponse response = productVariantService.getById(productId, variantId);
        return ResponseEntity.ok(BaseResponse.success(response, "Product variant retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_PRODUCT_VIEW')")
    public ResponseEntity<BaseResponse<PageResponse<ProductVariantResponse>>> getAll(
            @PathVariable String productId,
            @PageableDefault(size = 20, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable) {
        log.info("Getting product variants: productId={}", productId);
        PageResponse<ProductVariantResponse> response = productVariantService.getAll(productId, pageable);
        return ResponseEntity.ok(BaseResponse.success(response, "Product variants retrieved successfully"));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_VIEW')")
    public ResponseEntity<BaseResponse<List<ProductVariantResponse>>> getAllActive(@PathVariable String productId) {
        log.info("Getting active product variants: productId={}", productId);
        List<ProductVariantResponse> response = productVariantService.getAllActive(productId);
        return ResponseEntity.ok(BaseResponse.success(response, "Active product variants retrieved successfully"));
    }
}
