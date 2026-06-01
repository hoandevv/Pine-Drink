package com.hoandev.pinedrink.service;

public interface AccessScopeService {
    void assertSystemAccess();

    void assertCanAccessBranch(String branchId);

    void assertCanManageBranch(String branchId);

    void assertCanDeleteBranch(String branchId);
}
