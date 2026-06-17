package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.Branch.BranchProductAvailabilityResponse;
import com.hoandev.pinedrink.entity.dto.response.Branch.BranchToppingAvailabilityResponse;
import com.hoandev.pinedrink.service.BranchAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller công khai cung cấp các API kiểm tra tình trạng tồn kho (availability)
 * của một chi nhánh (branch). Các endpoint trả về tình trạng của sản phẩm và
 * topping cho mục đích đọc công khai (không yêu cầu xác thực ở tầng này).
 *
 * Base path: /api/v1/branches/{branchId}/availability
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/availability")
@RequiredArgsConstructor
@Slf4j
public class PublicBranchAvailabilityController {

    private final BranchAvailabilityService branchAvailabilityService;

    /**
     * Lấy thông tin availability của 1 sản phẩm tại chi nhánh.
     *
     * @param branchId id của chi nhánh
     * @param id       id của sản phẩm
     * @return ResponseEntity chứa BaseResponse với dữ liệu BranchProductAvailabilityResponse
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<BaseResponse<BranchProductAvailabilityResponse>> getProductAvailability(
            @PathVariable String branchId,
            @PathVariable String id) {
        log.info("Getting public branch product availability: branchId={}, id={}", branchId, id);
        BranchProductAvailabilityResponse response = branchAvailabilityService.getPublicProductAvailability(branchId, id);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch product availability retrieved successfully"));
    }

    /**
     * Lấy danh sách availability của tất cả sản phẩm tại chi nhánh.
     *
     * @param branchId id của chi nhánh
     * @return ResponseEntity chứa BaseResponse với danh sách BranchProductAvailabilityResponse
     */
    @GetMapping("/products")
    public ResponseEntity<BaseResponse<List<BranchProductAvailabilityResponse>>> getProductAvailabilities(
            @PathVariable String branchId) {
        log.info("Getting public branch product availabilities: branchId={}", branchId);
        List<BranchProductAvailabilityResponse> response = branchAvailabilityService.getPublicProductAvailabilities(branchId);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch product availabilities retrieved successfully"));
    }

    /**
     * Lấy thông tin availability của 1 topping tại chi nhánh.
     *
     * @param branchId id của chi nhánh
     * @param id       id của topping
     * @return ResponseEntity chứa BaseResponse với dữ liệu BranchToppingAvailabilityResponse
     */
    @GetMapping("/toppings/{id}")
    public ResponseEntity<BaseResponse<BranchToppingAvailabilityResponse>> getToppingAvailability(
            @PathVariable String branchId,
            @PathVariable String id) {
        log.info("Getting public branch topping availability: branchId={}, id={}", branchId, id);
        BranchToppingAvailabilityResponse response = branchAvailabilityService.getPublicToppingAvailability(branchId, id);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch topping availability retrieved successfully"));
    }

    /**
     * Lấy danh sách availability của tất cả topping tại chi nhánh.
     *
     * @param branchId id của chi nhánh
     * @return ResponseEntity chứa BaseResponse với danh sách BranchToppingAvailabilityResponse
     */
    @GetMapping("/toppings")
    public ResponseEntity<BaseResponse<List<BranchToppingAvailabilityResponse>>> getToppingAvailabilities(
            @PathVariable String branchId) {
        log.info("Getting public branch topping availabilities: branchId={}", branchId);
        List<BranchToppingAvailabilityResponse> response = branchAvailabilityService.getPublicToppingAvailabilities(branchId);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch topping availabilities retrieved successfully"));
    }
}
