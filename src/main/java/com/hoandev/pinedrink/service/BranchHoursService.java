package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Branch.CreateBranchHoursRequest;
import com.hoandev.pinedrink.entity.dto.request.Branch.UpdateBranchHoursRequest;
import com.hoandev.pinedrink.entity.dto.response.Branch.BranchHoursResponse;

import java.util.List;

/**
 * Quản lý giờ hoạt động của chi nhánh
 */
public interface BranchHoursService {
    /**
     * Tạo giờ hoạt động mới cho chi nhánh
     *
     * @param branchId ID chi nhánh
     * @param request Thông tin giờ hoạt động
     * @return Thông tin giờ hoạt động vừa tạo
     */
    BranchHoursResponse create(String branchId, CreateBranchHoursRequest request);

    /**
     * Cập nhật giờ hoạt động
     *
     * @param branchId ID chi nhánh
     * @param id ID giờ hoạt động cần cập nhật
     * @param request Thông tin cập nhật
     * @return Thông tin giờ hoạt động sau cập nhật
     */
    BranchHoursResponse update(String branchId, String id, UpdateBranchHoursRequest request);

    /**
     * Xóa giờ hoạt động
     *
     * @param branchId ID chi nhánh
     * @param id ID giờ hoạt động cần xóa
     */
    void delete(String branchId, String id);

    /**
     * Lấy giờ hoạt động theo ID
     *
     * @param branchId ID chi nhánh
     * @param id ID giờ hoạt động
     * @return Thông tin giờ hoạt động
     */
    BranchHoursResponse getById(String branchId, String id);

    /**
     * Lấy tất cả giờ hoạt động của chi nhánh
     *
     * @param branchId ID chi nhánh
     * @return Danh sách giờ hoạt động
     */
    List<BranchHoursResponse> getByBranch(String branchId);
}
