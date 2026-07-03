package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.Product.CreateProductRequest;
import com.hoandev.pinedrink.entity.dto.request.Product.UpdateProductRequest;
import com.hoandev.pinedrink.entity.dto.request.Product.UpdateProductStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductVariantResponse;
import com.hoandev.pinedrink.service.ProductService;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final ProductVariantService productVariantService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_PRODUCT_CREATE')")
    public ResponseEntity<BaseResponse<ProductResponse>> create(@Valid @RequestBody CreateProductRequest request) {
        log.info("Creating product for categoryId={}", request.getCategoryId());
        ProductResponse response = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Product created successfully"));
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_CREATE')")
    public ResponseEntity<BaseResponse<ProductResponse>> createWithImage(
            @Valid @RequestPart("request") CreateProductRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("Creating product with image for categoryId={}", request.getCategoryId());
        ProductResponse response = productService.create(request, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Product created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_UPDATE')")
    public ResponseEntity<BaseResponse<ProductResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateProductRequest request) {
        log.info("Updating product: id={}", id);
        ProductResponse response = productService.update(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Product updated successfully"));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_UPDATE')")
    public ResponseEntity<BaseResponse<ProductResponse>> updateWithImage(
            @PathVariable String id,
            @Valid @RequestPart("request") UpdateProductRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("Updating product with image: id={}", id);
        ProductResponse response = productService.update(id, request, file);
        return ResponseEntity.ok(BaseResponse.success(response, "Product updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_UPDATE')")
    public ResponseEntity<BaseResponse<ProductResponse>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateProductStatusRequest request) {
        log.info("Updating product status: id={}, status={}", id, request.getStatus());
        ProductResponse response = productService.updateStatus(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Product status updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_DELETE')")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable String id) {
        log.info("Deleting product: id={}", id);
        productService.delete(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Product deleted successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<ProductResponse>> getById(@PathVariable String id) {
        log.info("Getting product: id={}", id);
        ProductResponse response = productService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Product retrieved successfully"));
    }

    @GetMapping("/variants/active")
    public ResponseEntity<BaseResponse<java.util.List<ProductVariantResponse>>> getAllActiveVariants() {
        log.info("Getting active variants for active products");
        java.util.List<ProductVariantResponse> response = productVariantService.getAllActiveForProducts();
        return ResponseEntity.ok(BaseResponse.success(response, "Active product variants retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<ProductResponse>>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Getting products: keyword={}, categoryId={}, status={}", keyword, categoryId, status);
        PageResponse<ProductResponse> response = productService.getAll(keyword, categoryId, status, pageable);
        return ResponseEntity.ok(BaseResponse.success(response, "Products retrieved successfully"));
    }
}
