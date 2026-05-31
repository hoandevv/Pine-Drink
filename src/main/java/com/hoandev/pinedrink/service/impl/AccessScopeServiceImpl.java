package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.AccountRoleAssignment;
import com.hoandev.pinedrink.entity.Branch;
import com.hoandev.pinedrink.entity.Scope;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.repository.AccountRoleAssignmentRepository;
import com.hoandev.pinedrink.repository.BranchRepository;
import com.hoandev.pinedrink.security.UserPrincipal;
import com.hoandev.pinedrink.service.AccessScopeService;
import com.hoandev.pinedrink.utils.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AccessScopeServiceImpl implements AccessScopeService {

    private final AccountRoleAssignmentRepository assignmentRepository;
    private final BranchRepository branchRepository;

    @Override
    @Transactional(readOnly = true)
    public void assertCanAccessBrand(String brandId) {
        AccessScopeContext scope = resolveAccessScope();
        if (scope.fullAccess() || scope.brandIds().contains(brandId)) {
            return;
        }
        throw new BaseException(ErrorCode.AUTH_007);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCanManageBrand(String brandId) {
        AccessScopeContext scope = resolveAccessScope();
        if (scope.fullAccess() || scope.brandIds().contains(brandId)) {
            return;
        }
        throw new BaseException(ErrorCode.AUTH_007);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCanAccessBranch(String branchId) {
        AccessScopeContext scope = resolveAccessScope();
        if (scope.fullAccess() || scope.branchIds().contains(branchId)) {
            return;
        }

        Branch branch = getBranchOrThrow(branchId);
        String brandId = branch.getBrand() != null ? branch.getBrand().getId() : null;
        if (brandId != null && scope.brandIds().contains(brandId)) {
            return;
        }
        throw new BaseException(ErrorCode.AUTH_007);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCanManageBranch(String branchId) {
        assertCanAccessBranch(branchId);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCanDeleteBranch(String branchId) {
        AccessScopeContext scope = resolveAccessScope();
        if (scope.fullAccess()) {
            return;
        }

        Branch branch = getBranchOrThrow(branchId);
        String brandId = branch.getBrand() != null ? branch.getBrand().getId() : null;
        if (brandId != null && scope.brandIds().contains(brandId)) {
            return;
        }
        throw new BaseException(ErrorCode.AUTH_007);
    }

    private AccessScopeContext resolveAccessScope() {
        UserPrincipal principal = getCurrentPrincipal();
        List<AccountRoleAssignment> assignments = assignmentRepository.findActiveAssignmentsByAccountId(
                principal.getId(), LocalDateTime.now());

        boolean fullAccess = assignments.stream().anyMatch(assignment ->
                Constants.SCOPE_SYSTEM.equals(assignment.getScope().getScopeType())
        );
        if (fullAccess) {
            return new AccessScopeContext(true, Set.of(), Set.of());
        }

        Set<String> brandIds = new HashSet<>();
        Set<String> branchIds = new HashSet<>();
        for (AccountRoleAssignment assignment : assignments) {
            Scope scope = assignment.getScope();
            if (scope.getBrand() != null && scope.getBrand().getId() != null) {
                brandIds.add(scope.getBrand().getId());
            }
            if (scope.getBranch() != null && scope.getBranch().getId() != null) {
                branchIds.add(scope.getBranch().getId());
            }
        }
        return new AccessScopeContext(false, brandIds, branchIds);
    }

    private UserPrincipal getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BaseException(ErrorCode.AUTH_003);
        }
        return principal;
    }

    private Branch getBranchOrThrow(String branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new BaseException(ErrorCode.BRANCH_001));
    }

    private record AccessScopeContext(boolean fullAccess, Set<String> brandIds, Set<String> branchIds) {
    }
}
