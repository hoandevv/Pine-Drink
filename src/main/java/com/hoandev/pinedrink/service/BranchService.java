package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Branch.CreateBranchRequest;
import com.hoandev.pinedrink.entity.dto.request.Branch.UpdateBranchRequest;
import com.hoandev.pinedrink.entity.dto.request.Branch.UpdateBranchStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.Branch.BranchResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface BranchService {
    BranchResponse create(CreateBranchRequest request);

    BranchResponse update(String id, UpdateBranchRequest request);

    BranchResponse updateStatus(String id, UpdateBranchStatusRequest request);

    void delete(String id);

    BranchResponse getById(String id);

    PageResponse<BranchResponse> getAll(Pageable pageable);

    PageResponse<BranchResponse> getAllActive(Pageable pageable);
}
