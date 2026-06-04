package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.ProductTopping.AssignProductToppingRequest;
import com.hoandev.pinedrink.entity.dto.request.ProductTopping.UpdateProductToppingRequest;
import com.hoandev.pinedrink.entity.dto.request.ProductTopping.UpdateProductToppingStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductToppingResponse;
import com.hoandev.pinedrink.service.ProductToppingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/api/v1/products/{productId}/toppings")
@RequiredArgsConstructor
@Slf4j
public class ProductToppingController {

    private final ProductToppingService productToppingService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_TOPPING_CREATE')")
    public ResponseEntity<BaseResponse<ProductToppingResponse>> assign(
            @PathVariable String productId,
            @Valid @RequestBody AssignProductToppingRequest request) {
        log.info("Assigning topping to product: productId={}", productId);
        ProductToppingResponse response = productToppingService.assign(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Product topping assigned successfully"));
    }

    @PutMapping("/{productToppingId}")
    @PreAuthorize("hasAuthority('PERM_TOPPING_UPDATE')")
    public ResponseEntity<BaseResponse<ProductToppingResponse>> update(
            @PathVariable String productId,
            @PathVariable String productToppingId,
            @Valid @RequestBody UpdateProductToppingRequest request) {
        log.info("Updating product topping: productId={}, productToppingId={}", productId, productToppingId);
        ProductToppingResponse response = productToppingService.update(productId, productToppingId, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Product topping updated successfully"));
    }

    @PatchMapping("/{productToppingId}/status")
    @PreAuthorize("hasAuthority('PERM_TOPPING_UPDATE')")
    public ResponseEntity<BaseResponse<ProductToppingResponse>> updateStatus(
            @PathVariable String productId,
            @PathVariable String productToppingId,
            @Valid @RequestBody UpdateProductToppingStatusRequest request) {
        log.info("Updating product topping status: productId={}, productToppingId={}, status={}", productId, productToppingId, request.getStatus());
        ProductToppingResponse response = productToppingService.updateStatus(productId, productToppingId, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Product topping status updated successfully"));
    }

    @DeleteMapping("/{productToppingId}")
    @PreAuthorize("hasAuthority('PERM_TOPPING_DELETE')")
    public ResponseEntity<BaseResponse<Void>> delete(
            @PathVariable String productId,
            @PathVariable String productToppingId) {
        log.info("Deleting product topping: productId={}, productToppingId={}", productId, productToppingId);
        productToppingService.delete(productId, productToppingId);
        return ResponseEntity.ok(BaseResponse.success(null, "Product topping deleted successfully"));
    }

    @GetMapping("/{productToppingId}")
    @PreAuthorize("hasAuthority('PERM_TOPPING_VIEW')")
    public ResponseEntity<BaseResponse<ProductToppingResponse>> getById(
            @PathVariable String productId,
            @PathVariable String productToppingId) {
        log.info("Getting product topping: productId={}, productToppingId={}", productId, productToppingId);
        ProductToppingResponse response = productToppingService.getById(productId, productToppingId);
        return ResponseEntity.ok(BaseResponse.success(response, "Product topping retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_TOPPING_VIEW')")
    public ResponseEntity<BaseResponse<List<ProductToppingResponse>>> getAll(@PathVariable String productId) {
        log.info("Getting product toppings: productId={}", productId);
        List<ProductToppingResponse> response = productToppingService.getAll(productId);
        return ResponseEntity.ok(BaseResponse.success(response, "Product toppings retrieved successfully"));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('PERM_TOPPING_VIEW')")
    public ResponseEntity<BaseResponse<List<ProductToppingResponse>>> getAllActive(@PathVariable String productId) {
        log.info("Getting active product toppings: productId={}", productId);
        List<ProductToppingResponse> response = productToppingService.getAllActive(productId);
        return ResponseEntity.ok(BaseResponse.success(response, "Active product toppings retrieved successfully"));
    }
}
