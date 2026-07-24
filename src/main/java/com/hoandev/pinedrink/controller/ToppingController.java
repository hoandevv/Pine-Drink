package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.ProductTopping.CreateToppingRequest;
import com.hoandev.pinedrink.entity.dto.request.ProductTopping.UpdateToppingRequest;
import com.hoandev.pinedrink.entity.dto.request.ProductTopping.UpdateToppingStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ToppingResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ToppingSummaryResponse;
import com.hoandev.pinedrink.service.ToppingService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/toppings")
@Slf4j
public class ToppingController {

    private final ToppingService toppingService;

    public ToppingController(ToppingService toppingService) {
        this.toppingService = toppingService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_TOPPING_CREATE')")
    public ResponseEntity<BaseResponse<ToppingResponse>> create(@Valid @RequestBody CreateToppingRequest request) {
        log.info("Creating topping: name={}", request.getName());
        ToppingResponse response = toppingService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Topping created successfully"));
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('PERM_TOPPING_CREATE')")
    public ResponseEntity<BaseResponse<ToppingResponse>> createWithImage(
            @Valid @RequestPart("request") CreateToppingRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("Creating topping with image: name={}", request.getName());
        ToppingResponse response = toppingService.create(request, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Topping created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_TOPPING_UPDATE')")
    public ResponseEntity<BaseResponse<ToppingResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateToppingRequest request) {
        log.info("Updating topping: id={}", id);
        ToppingResponse response = toppingService.update(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Topping updated successfully"));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('PERM_TOPPING_UPDATE')")
    public ResponseEntity<BaseResponse<ToppingResponse>> updateWithImage(
            @PathVariable String id,
            @Valid @RequestPart("request") UpdateToppingRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("Updating topping with image: id={}", id);
        ToppingResponse response = toppingService.update(id, request, file);
        return ResponseEntity.ok(BaseResponse.success(response, "Topping updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERM_TOPPING_UPDATE')")
    public ResponseEntity<BaseResponse<ToppingResponse>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateToppingStatusRequest request) {
        ToppingResponse response = toppingService.updateStatus(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Topping status updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_TOPPING_DELETE')")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable String id) {
        log.info("Deleting topping: id={}", id);
        return ResponseEntity.ok(BaseResponse.success(null, "Topping deleted successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<ToppingResponse>> getById(@PathVariable String id) {
        ToppingResponse response = toppingService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Topping retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<ToppingSummaryResponse>>> getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ToppingSummaryResponse> response = toppingService.getAll(pageable);
        return ResponseEntity.ok(BaseResponse.success(response, "Toppings retrieved successfully"));
    }

    @GetMapping("/active")
    public ResponseEntity<BaseResponse<List<ToppingSummaryResponse>>> getAllActive() {
        List<ToppingSummaryResponse> response = toppingService.getAllActive();
        return ResponseEntity.ok(BaseResponse.success(response, "Active toppings retrieved successfully"));
    }
}
