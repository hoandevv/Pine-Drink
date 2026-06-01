package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.Scope;
import com.hoandev.pinedrink.security.scope.AccessScopeContext;

/**
 * Resolves and enforces account scope access across branch-aware services.
 */
public interface AccessScopeService {

    /**
     * Resolves scope grants for the current authenticated account.
     *
     * @return scope context for the current account
     */
    AccessScopeContext resolveCurrentScope();
    /**Ensures that the current account has SYSTEM-level access.
     *
     */
    void assertSystemAccess();
    /** Ensures that the current account can view or use data from the given branch.
     *
     */
    void assertCanAccessBranch(String branchId);
    /**
     * Ensures that the current account can manage (create/update/delete) data in the given branch.
     * */
    void assertCanManageBranch(String branchId);
    /**
     * Ensures that the current account can manage data within the given branch.
     * */
    void assertCanDeleteBranch(String branchId);
    /**
     * Ensures that the current account can delete or deactivate the given branch.
     * */
    void assertCanAccessAccount(String targetAccountId);
    /**
     * Ensures that the current account can access the target account.
     * */
    void assertCanAccessScope(Scope scope);
    /**
     * Ensures that the current account can access the target scope.
     * */
    void assertCanManageTargetScope(String scopeType, String branchId);
}
