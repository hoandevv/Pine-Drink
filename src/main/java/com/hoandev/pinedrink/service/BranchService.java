package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Branch.CreateBranchRequest;
import com.hoandev.pinedrink.entity.dto.request.Branch.UpdateBranchRequest;
import com.hoandev.pinedrink.entity.dto.response.Branch.BranchResponse;

import java.util.List;

/**
 * Service interface for managing branches.
 */
public interface BranchService {
    /**
     * Creates a new branch.
     *
     * @param request the request containing the branch details
     * @return the created branch
     */
    BranchResponse create(CreateBranchRequest request);

    /**
     * Updates an existing branch.
     *
     * @param id the ID of the branch to update
     * @param request the request containing the updated branch details
     * @return the updated branch
     */
    BranchResponse update(String id, UpdateBranchRequest request);

    /**
     * Deletes a branch (soft delete by setting status to INACTIVE).
     *
     * @param id the ID of the branch to delete
     */
    void delete(String id);

    /**
     * Retrieves a branch by its ID.
     *
     * @param id the ID of the branch to retrieve
     * @return the branch
     */
    BranchResponse getById(String id);

    /**
     * Retrieves all branches for a specific brand.
     *
     * @param brandId the brand ID
     * @return list of branches
     */
    List<BranchResponse> getAllByBrandId(String brandId);

    /**
     * Retrieves all active branches for a specific brand.
     *
     * @param brandId the brand ID
     * @return list of active branches
     */
    List<BranchResponse> getAllActiveByBrandId(String brandId);
}
