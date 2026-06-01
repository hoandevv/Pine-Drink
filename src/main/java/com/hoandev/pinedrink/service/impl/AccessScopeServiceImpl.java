package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.AccountRoleAssignment;
import com.hoandev.pinedrink.entity.Scope;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.repository.AccountRoleAssignmentRepository;
import com.hoandev.pinedrink.security.UserPrincipal;
import com.hoandev.pinedrink.security.scope.AccessScopeContext;
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

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public AccessScopeContext resolveCurrentScope() {
        return resolveAccessScope();
    }

    @Override
    @Transactional(readOnly = true)
    public void assertSystemAccess() {
        if (resolveAccessScope().fullAccess()) {
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
        assertCanAccessBranch(branchId);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCanAccessAccount(String targetAccountId) {
        AccessScopeContext accessScope = resolveAccessScope();
        if (accessScope.fullAccess()) {
            return;
        }
        if (hasSharedBranchScope(targetAccountId, accessScope.branchIds())) {
            return;
        }
        throw new BaseException(ErrorCode.AUTH_007);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCanAccessScope(Scope scope) {
        AccessScopeContext accessScope = resolveAccessScope();
        if (accessScope.fullAccess()) {
            return;
        }
        if (scope.getBranch() == null || !accessScope.branchIds().contains(scope.getBranch().getId())) {
            throw new BaseException(ErrorCode.AUTH_007);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCanManageTargetScope(String scopeType, String branchId) {
        AccessScopeContext accessScope = resolveAccessScope();
        if (accessScope.fullAccess()) {
            return;
        }
        if (!Constants.SCOPE_BRANCH.equals(normalizeScopeType(scopeType))) {
            throw new BaseException(ErrorCode.AUTH_007);
        }
        String normalizedBranchId = normalizeNullable(branchId);
        if (normalizedBranchId == null || !accessScope.branchIds().contains(normalizedBranchId)) {
            throw new BaseException(ErrorCode.AUTH_007);
        }
    }

    /**
     * Checks whether a target account has at least one active BRANCH scope shared with the caller.
     *
     * @param targetAccountId account being accessed
     * @param allowedBranchIds branch IDs granted to the current caller
     * @return true when target account belongs to one allowed branch
     */
    private boolean hasSharedBranchScope(String targetAccountId, Set<String> allowedBranchIds) {
        if (allowedBranchIds.isEmpty()) {
            return false;
        }
        return assignmentRepository.findActiveAssignmentsByAccountId(targetAccountId, LocalDateTime.now()).stream()
                .map(AccountRoleAssignment::getScope)
                .filter(scope -> Constants.SCOPE_BRANCH.equals(scope.getScopeType()))
                .filter(scope -> scope.getBranch() != null)
                .map(scope -> scope.getBranch().getId())
                .anyMatch(allowedBranchIds::contains);
    }

    private AccessScopeContext resolveAccessScope() {
        UserPrincipal principal = getCurrentPrincipal();
        List<AccountRoleAssignment> assignments = assignmentRepository.findActiveAssignmentsByAccountId(
                principal.getId(), LocalDateTime.now());

        boolean fullAccess = assignments.stream().anyMatch(assignment ->
                Constants.SCOPE_SYSTEM.equals(assignment.getScope().getScopeType())
        );
        if (fullAccess) {
            return new AccessScopeContext(true, Set.of());
        }

        Set<String> branchIds = new HashSet<>();
        for (AccountRoleAssignment assignment : assignments) {
            Scope scope = assignment.getScope();
            if (scope.getBranch() != null && scope.getBranch().getId() != null) {
                branchIds.add(scope.getBranch().getId());
            }
        }
        return new AccessScopeContext(false, branchIds);
    }

    private UserPrincipal getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BaseException(ErrorCode.AUTH_003);
        }
        return principal;
    }

    private String normalizeScopeType(String scopeType) {
        String normalized = normalizeNullable(scopeType);
        if (normalized == null) {
            return Constants.SCOPE_SYSTEM;
        }
        String upper = normalized.toUpperCase();
        if (!Constants.SCOPE_SYSTEM.equals(upper) && !Constants.SCOPE_BRANCH.equals(upper)) {
            throw new BaseException(ErrorCode.COM_004, "Unsupported scope type: " + scopeType);
        }
        return upper;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
