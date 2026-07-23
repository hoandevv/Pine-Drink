package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Branch.*;
import com.hoandev.pinedrink.entity.dto.response.Branch.*;

import java.util.List;

/**
 * Service xử lý trạng thái khả dụng (availability) của sản phẩm và topping theo chi nhánh.
 *
 * Ngắn gọn: cung cấp CRUD cho availability ở cả phạm vi internal (quản trị) và public (khách hàng).
 */
public interface BranchAvailabilityService {
    /**
     * Tạo availability cho sản phẩm thuộc chi nhánh.
     * @param branchId id của chi nhánh
     * @param request dữ liệu tạo
     * @return đối tượng phản hồi availability vừa tạo
     */
    BranchProductAvailabilityResponse createProductAvailability(String branchId, CreateBranchProductAvailabilityRequest request);

    /**
     * Cập nhật availability sản phẩm theo id.
     * @param branchId id của chi nhánh
     * @param id id availability cần cập nhật
     * @param request dữ liệu cập nhật
     * @return đối tượng phản hồi sau khi cập nhật
     */
    BranchProductAvailabilityResponse updateProductAvailability(String branchId, String id, UpdateBranchProductAvailabilityRequest request);

    /**
     * Xóa availability sản phẩm.
     * @param branchId id của chi nhánh
     * @param id id availability cần xóa
     */
    void deleteProductAvailability(String branchId, String id);

    /**
     * Lấy chi tiết availability của một sản phẩm theo id (private/internal).
     * @param branchId id của chi nhánh
     * @param id id availability
     * @return đối tượng availability
     */
    BranchProductAvailabilityResponse getProductAvailability(String branchId, String id);

    /**
     * Lấy danh sách tất cả availability sản phẩm của chi nhánh (private/internal).
     * @param branchId id của chi nhánh
     * @return danh sách availability
     */
    List<BranchProductAvailabilityResponse> getProductAvailabilities(String branchId);

    /**
     * Lấy chi tiết availability sản phẩm cho client/public.
     * @param branchId id của chi nhánh
     * @param id id availability
     * @return đối tượng availability (phiên bản public)
     */
    BranchProductAvailabilityResponse getPublicProductAvailability(String branchId, String id);

    /**
     * Lấy danh sách availability sản phẩm công khai cho client.
     * @param branchId id của chi nhánh
     * @return danh sách availability (public)
     */
    List<BranchProductAvailabilityResponse> getPublicProductAvailabilities(String branchId);

    /**
     * Tạo availability cho topping thuộc chi nhánh.
     * @param branchId id của chi nhánh
     * @param request dữ liệu tạo
     * @return đối tượng phản hồi availability vừa tạo
     */
    BranchToppingAvailabilityResponse createToppingAvailability(String branchId, CreateBranchToppingAvailabilityRequest request);

    /**
     * Cập nhật availability topping theo id.
     * @param branchId id của chi nhánh
     * @param id id availability
     * @param request dữ liệu cập nhật
     * @return đối tượng phản hồi sau khi cập nhật
     */
    BranchToppingAvailabilityResponse updateToppingAvailability(String branchId, String id, UpdateBranchToppingAvailabilityRequest request);

    /**
     * Xóa availability topping.
     * @param branchId id của chi nhánh
     * @param id id availability cần xóa
     */
    void deleteToppingAvailability(String branchId, String id);

    /**
     * Lấy chi tiết availability của một topping theo id (private/internal).
     * @param branchId id của chi nhánh
     * @param id id availability
     * @return đối tượng availability
     */
    BranchToppingAvailabilityResponse getToppingAvailability(String branchId, String id);

    /**
     * Lấy danh sách tất cả availability topping của chi nhánh (private/internal).
     * @param branchId id của chi nhánh
     * @return danh sách availability
     */
    List<BranchToppingAvailabilityResponse> getToppingAvailabilities(String branchId);

    /**
     * Lấy chi tiết availability topping cho client/public.
     * @param branchId id của chi nhánh
     * @param id id availability
     * @return đối tượng availability (phiên bản public)
     */
    BranchToppingAvailabilityResponse getPublicToppingAvailability(String branchId, String id);

    /**
     * Lấy danh sách availability topping công khai cho client.
     * @param branchId id của chi nhánh
     * @return danh sách availability (public)
     */
    List<BranchToppingAvailabilityResponse> getPublicToppingAvailabilities(String branchId);
}
