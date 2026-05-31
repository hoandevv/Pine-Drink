package com.hoandev.pinedrink.service;

public interface AccessScopeService {

    void assertCanAccessBrand(String brandId);

    void assertCanManageBrand(String brandId);

    void assertCanAccessBranch(String branchId);

    void assertCanManageBranch(String branchId);

    void assertCanDeleteBranch(String branchId);
}
