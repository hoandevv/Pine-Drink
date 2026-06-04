package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Branch.CreateBranchHoursRequest;
import com.hoandev.pinedrink.entity.dto.request.Branch.UpdateBranchHoursRequest;
import com.hoandev.pinedrink.entity.dto.response.Branch.BranchHoursResponse;

import java.util.List;

public interface BranchHoursService {
    BranchHoursResponse create(String branchId, CreateBranchHoursRequest request);

    BranchHoursResponse update(String branchId, String id, UpdateBranchHoursRequest request);

    void delete(String branchId, String id);

    
    BranchHoursResponse getById(String branchId, String id);

    List<BranchHoursResponse> getByBranch(String branchId);
}
