package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.Branch.*;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.Branch.*;
import com.hoandev.pinedrink.service.BranchAvailabilityService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller cho quản lý tính sẵn có của sản phẩm và topping tại chi nhánh.
 * Cung cấp các API để tạo, cập nhật, xóa và lấy thông tin sẵn có.
 */
@RestController
@RequestMapping("/api/v1/admin/branches/{branchId}/availability")
@Slf4j
public class BranchAvailabilityController {
    private final BranchAvailabilityService branchAvailabilityService;

    public BranchAvailabilityController(BranchAvailabilityService branchAvailabilityService) {
        this.branchAvailabilityService = branchAvailabilityService;
    }

    /**
     * Tạo mới sẵn có sản phẩm cho chi nhánh.
     *
     * @param branchId ID của chi nhánh
     * @param request thông tin sẵn có sản phẩm
     * @return chi nhánh sản phẩm sẵn có vừa tạo
     */
    @PostMapping("/products")
    @PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
    public ResponseEntity<BaseResponse<BranchProductAvailabilityResponse>> createProductAvailability(
            @PathVariable String branchId,
            @Valid @RequestBody CreateBranchProductAvailabilityRequest request) {
        log.info("Creating branch product availability: branchId={}", branchId);
        BranchProductAvailabilityResponse response = branchAvailabilityService.createProductAvailability(branchId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Branch product availability created successfully"));
    }

    /**
     * Cập nhật sẵn có sản phẩm của chi nhánh.
     *
     * @param branchId ID của chi nhánh
     * @param id ID của sẵn có sản phẩm
     * @param request thông tin cập nhật
     * @return sẵn có sản phẩm đã cập nhật
     */
    @PutMapping("/products/{id}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
    public ResponseEntity<BaseResponse<BranchProductAvailabilityResponse>> updateProductAvailability(
            @PathVariable String branchId,
            @PathVariable String id,
            @Valid @RequestBody UpdateBranchProductAvailabilityRequest request) {
        log.info("Updating branch product availability: branchId={}, id={}", branchId, id);
        BranchProductAvailabilityResponse response = branchAvailabilityService.updateProductAvailability(branchId, id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch product availability updated successfully"));
    }

    /**
     * Xóa sẵn có sản phẩm của chi nhánh.
     *
     * @param branchId ID của chi nhánh
     * @param id ID của sẵn có sản phẩm
     * @return thông báo xóa thành công
     */
    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
    public ResponseEntity<BaseResponse<Void>> deleteProductAvailability(@PathVariable String branchId, @PathVariable String id) {
        log.info("Deleting branch product availability: branchId={}, id={}", branchId, id);
        branchAvailabilityService.deleteProductAvailability(branchId, id);
        return ResponseEntity.ok(BaseResponse.success(null, "Branch product availability deleted successfully"));
    }

    /**
     * Lấy thông tin sẵn có sản phẩm theo ID.
     *
     * @param branchId ID của chi nhánh
     * @param id ID của sẵn có sản phẩm
     * @return thông tin sẵn có sản phẩm
     */
    @GetMapping("/products/{id}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_VIEW')")
    public ResponseEntity<BaseResponse<BranchProductAvailabilityResponse>> getProductAvailability(@PathVariable String branchId, @PathVariable String id) {
        log.info("Getting branch product availability: branchId={}, id={}", branchId, id);
        BranchProductAvailabilityResponse response = branchAvailabilityService.getProductAvailability(branchId, id);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch product availability retrieved successfully"));
    }

    /**
     * Lấy danh sách tất cả sẵn có sản phẩm của chi nhánh.
     *
     * @param branchId ID của chi nhánh
     * @return danh sách sẵn có sản phẩm
     */
    @GetMapping("/products")
    @PreAuthorize("hasAuthority('PERM_BRANCH_VIEW')")
    public ResponseEntity<BaseResponse<List<BranchProductAvailabilityResponse>>> getProductAvailabilities(@PathVariable String branchId) {
        log.info("Getting branch product availabilities: branchId={}", branchId);
        List<BranchProductAvailabilityResponse> response = branchAvailabilityService.getProductAvailabilities(branchId);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch product availabilities retrieved successfully"));
    }

    /**
     * Tạo mới sẵn có topping cho chi nhánh.
     *
     * @param branchId ID của chi nhánh
     * @param request thông tin sẵn có topping
     * @return chi nhánh topping sẵn có vừa tạo
     */
    @PostMapping("/toppings")
    @PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
    public ResponseEntity<BaseResponse<BranchToppingAvailabilityResponse>> createToppingAvailability(
            @PathVariable String branchId,
            @Valid @RequestBody CreateBranchToppingAvailabilityRequest request) {
        log.info("Creating branch topping availability: branchId={}", branchId);
        BranchToppingAvailabilityResponse response = branchAvailabilityService.createToppingAvailability(branchId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Branch topping availability created successfully"));
    }

    /**
     * Cập nhật sẵn có topping của chi nhánh.
     *
     * @param branchId ID của chi nhánh
     * @param id ID của sẵn có topping
     * @param request thông tin cập nhật
     * @return sẵn có topping đã cập nhật
     */
    @PutMapping("/toppings/{id}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
    public ResponseEntity<BaseResponse<BranchToppingAvailabilityResponse>> updateToppingAvailability(
            @PathVariable String branchId,
            @PathVariable String id,
            @Valid @RequestBody UpdateBranchToppingAvailabilityRequest request) {
        log.info("Updating branch topping availability: branchId={}, id={}", branchId, id);
        BranchToppingAvailabilityResponse response = branchAvailabilityService.updateToppingAvailability(branchId, id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch topping availability updated successfully"));
    }

    /**
     * Xóa sẵn có topping của chi nhánh.
     *
     * @param branchId ID của chi nhánh
     * @param id ID của sẵn có topping
     * @return thông báo xóa thành công
     */
    @DeleteMapping("/toppings/{id}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_UPDATE')")
    public ResponseEntity<BaseResponse<Void>> deleteToppingAvailability(@PathVariable String branchId, @PathVariable String id) {
        log.info("Deleting branch topping availability: branchId={}, id={}", branchId, id);
        branchAvailabilityService.deleteToppingAvailability(branchId, id);
        return ResponseEntity.ok(BaseResponse.success(null, "Branch topping availability deleted successfully"));
    }

    /**
     * Lấy thông tin sẵn có topping theo ID.
     *
     * @param branchId ID của chi nhánh
     * @param id ID của sẵn có topping
     * @return thông tin sẵn có topping
     */
    @GetMapping("/toppings/{id}")
    @PreAuthorize("hasAuthority('PERM_BRANCH_VIEW')")
    public ResponseEntity<BaseResponse<BranchToppingAvailabilityResponse>> getToppingAvailability(@PathVariable String branchId, @PathVariable String id) {
        log.info("Getting branch topping availability: branchId={}, id={}", branchId, id);
        BranchToppingAvailabilityResponse response = branchAvailabilityService.getToppingAvailability(branchId, id);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch topping availability retrieved successfully"));
    }

    /**
     * Lấy danh sách tất cả sẵn có topping của chi nhánh.
     *
     * @param branchId ID của chi nhánh
     * @return danh sách sẵn có topping
     */
    @GetMapping("/toppings")
    @PreAuthorize("hasAuthority('PERM_BRANCH_VIEW')")
    public ResponseEntity<BaseResponse<List<BranchToppingAvailabilityResponse>>> getToppingAvailabilities(@PathVariable String branchId) {
        log.info("Getting branch topping availabilities: branchId={}", branchId);
        List<BranchToppingAvailabilityResponse> response = branchAvailabilityService.getToppingAvailabilities(branchId);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch topping availabilities retrieved successfully"));
    }

}
