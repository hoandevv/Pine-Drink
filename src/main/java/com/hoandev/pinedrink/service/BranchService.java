package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Branch.CreateBranchRequest;
import com.hoandev.pinedrink.entity.dto.request.Branch.UpdateBranchRequest;
import com.hoandev.pinedrink.entity.dto.request.Branch.UpdateBranchStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.Branch.BranchResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

/**
 * Dịch vụ quản lý chi nhánh (Branch).
 * Cung cấp các chức năng CRUD và truy vấn thông tin chi nhánh.
 */
public interface BranchService {
    /**
     * Tạo mới một chi nhánh.
     *
     * @param request thông tin chi nhánh cần tạo
     * @return thông tin chi nhánh vừa tạo
     */
    BranchResponse create(CreateBranchRequest request);

    /**
     * Cập nhật thông tin chi nhánh.
     *
     * @param id mã chi nhánh
     * @param request thông tin chi nhánh cần cập nhật
     * @return thông tin chi nhánh đã cập nhật
     */
    BranchResponse update(String id, UpdateBranchRequest request);

    /**
     * Cập nhật trạng thái chi nhánh (kích hoạt/vô hiệu hóa).
     *
     * @param id mã chi nhánh
     * @param request yêu cầu cập nhật trạng thái
     * @return thông tin chi nhánh đã cập nhật
     */
    BranchResponse updateStatus(String id, UpdateBranchStatusRequest request);

    /**
     * Xóa một chi nhánh.
     *
     * @param id mã chi nhánh cần xóa
     */
    void delete(String id);

    /**
     * Lấy thông tin chi nhánh theo ID.
     *
     * @param id mã chi nhánh
     * @return thông tin chi nhánh
     */
    BranchResponse getById(String id);

    /**
     * Lấy danh sách tất cả chi nhánh (phân trang).
     *
     * @param pageable thông tin phân trang
     * @return danh sách chi nhánh
     */
    PageResponse<BranchResponse> getAll(Pageable pageable);

    /**
     * Lấy danh sách chi nhánh đang hoạt động (phân trang).
     *
     * @param pageable thông tin phân trang
     * @return danh sách chi nhánh đang hoạt động
     */
    PageResponse<BranchResponse> getAllActive(Pageable pageable);
}
