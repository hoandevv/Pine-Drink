package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.Account;
import com.hoandev.pinedrink.entity.AccountRoleAssignment;
import com.hoandev.pinedrink.entity.Branch;
import com.hoandev.pinedrink.entity.Brand;
import com.hoandev.pinedrink.entity.CustomerProfile;
import com.hoandev.pinedrink.entity.Role;
import com.hoandev.pinedrink.entity.Scope;
import com.hoandev.pinedrink.entity.dto.request.Account.AdminResetPasswordRequest;
import com.hoandev.pinedrink.entity.dto.request.Account.AssignRoleRequest;
import com.hoandev.pinedrink.entity.dto.request.Account.CreateAccountRequest;
import com.hoandev.pinedrink.entity.dto.request.Account.UpdateAccountRequest;
import com.hoandev.pinedrink.entity.dto.request.Account.UpdateAccountStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.Account.AccountDetailResponse;
import com.hoandev.pinedrink.entity.dto.response.Account.AccountListItemResponse;
import com.hoandev.pinedrink.entity.dto.response.Account.AccountRoleAssignmentResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.AccountManagementMapper;
import com.hoandev.pinedrink.repository.AccountRepository;
import com.hoandev.pinedrink.repository.AccountRoleAssignmentRepository;
import com.hoandev.pinedrink.repository.BranchRepository;
import com.hoandev.pinedrink.repository.BrandRepository;
import com.hoandev.pinedrink.repository.CustomerProfileRepository;
import com.hoandev.pinedrink.repository.RefreshTokenRepository;
import com.hoandev.pinedrink.repository.RoleRepository;
import com.hoandev.pinedrink.repository.ScopeRepository;
import com.hoandev.pinedrink.security.UserPrincipal;
import com.hoandev.pinedrink.service.AccountService;
import com.hoandev.pinedrink.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountRoleAssignmentRepository assignmentRepository;
    private final RoleRepository roleRepository;
    private final ScopeRepository scopeRepository;
    private final BrandRepository brandRepository;
    private final BranchRepository branchRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountManagementMapper accountManagementMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AccountListItemResponse> searchAccounts(String keyword,
                                                               String status,
                                                               String roleCode,
                                                               String brandId,
                                                               Pageable pageable) {
        AccessScopeContext accessScope = resolveAccessScope();
        Specification<Account> specification = buildSearchSpecification(keyword, status, roleCode, brandId, accessScope);
        Page<Account> page = accountRepository.findAll(specification, pageable);

        Map<String, List<String>> rolesByAccountId = loadActiveRoles(page.getContent());
        List<AccountListItemResponse> content = page.getContent().stream()
                .map(account -> accountManagementMapper.toListItem(
                        account,
                        rolesByAccountId.getOrDefault(account.getId(), List.of())
                ))
                .toList();

        return PageResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDetailResponse getAccountDetail(String id) {
        Account account = getAccountOrThrow(id);
        assertCanAccessAccount(account);
        return buildAccountDetail(account);
    }

    @Override
    @Transactional
    public AccountDetailResponse createAccount(CreateAccountRequest request) {
        assertCanManageTargetScope(request.getScopeType(), request.getScopeBrandId(), request.getScopeBranchId(), request.getBrandId());

        validateUniqueConstraints(
                normalizeUsername(request.getUsername()),
                normalizeEmail(request.getEmail()),
                normalizeNullable(request.getPhone()),
                null
        );

        Account account = new Account();
        account.setUsername(normalizeUsername(request.getUsername()));
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setFullName(request.getFullName().trim());
        account.setEmail(normalizeEmail(request.getEmail()));
        account.setPhone(normalizeNullable(request.getPhone()));
        account.setAvatarUrl(normalizeNullable(request.getAvatarUrl()));
        account.setStatus(normalizeStatus(request.getStatus()));
        account.setBrand(resolveAccountBrand(request.getBrandId(), request.getScopeType(), request.getScopeBrandId(), request.getScopeBranchId()));

        Account savedAccount = accountRepository.save(account);
        createAssignment(savedAccount, request.getRoleCode(), request.getScopeType(), request.getScopeBrandId(), request.getScopeBranchId(), request.getExpiresAt());

        log.info("Account created by admin: id={}, username={}", savedAccount.getId(), savedAccount.getUsername());
        return buildAccountDetail(savedAccount);
    }

    @Override
    @Transactional
    public AccountDetailResponse updateAccount(String id, UpdateAccountRequest request) {
        Account account = getAccountOrThrow(id);
        assertCanAccessAccount(account);

        String email = normalizeEmail(request.getEmail());
        String phone = normalizeNullable(request.getPhone());

        if (email != null && accountRepository.existsByEmailAndIdNot(email, account.getId())) {
            throw new BaseException(ErrorCode.AUTH_014);
        }
        if (phone != null && accountRepository.existsByPhoneAndIdNot(phone, account.getId())) {
            throw new BaseException(ErrorCode.AUTH_015);
        }

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            account.setFullName(request.getFullName().trim());
        }
        if (request.getEmail() != null) {
            account.setEmail(email);
        }
        if (request.getPhone() != null) {
            account.setPhone(phone);
        }
        if (request.getAvatarUrl() != null) {
            account.setAvatarUrl(request.getAvatarUrl().trim());
        }
        if (request.getBrandId() != null) {
            account.setBrand(resolveBrandOrNull(request.getBrandId()));
        }

        accountRepository.save(account);
        syncCustomerProfile(account);

        log.info("Account updated by admin: id={}", account.getId());
        return buildAccountDetail(account);
    }

    @Override
    @Transactional
    public AccountDetailResponse updateAccountStatus(String id, UpdateAccountStatusRequest request) {
        UserPrincipal principal = getCurrentPrincipal();
        if (principal.getId().equals(id)) {
            throw new BaseException(ErrorCode.COM_004, "You cannot change your own account status");
        }

        Account account = getAccountOrThrow(id);
        assertCanAccessAccount(account);
        account.setStatus(normalizeStatus(request.getStatus()));
        accountRepository.save(account);

        if (!Constants.STATUS_ACTIVE.equals(account.getStatus())) {
            refreshTokenRepository.deleteByAccountId(account.getId());
        }

        log.info("Account status updated by admin: id={}, status={}", account.getId(), account.getStatus());
        return buildAccountDetail(account);
    }

    @Override
    @Transactional
    public void adminResetPassword(String id, AdminResetPasswordRequest request) {
        Account account = getAccountOrThrow(id);
        assertCanAccessAccount(account);
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
        refreshTokenRepository.deleteByAccountId(account.getId());
        log.info("Account password reset by admin: id={}", account.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountRoleAssignmentResponse> getAccountRoles(String id) {
        Account account = getAccountOrThrow(id);
        assertCanAccessAccount(account);
        return assignmentRepository.findDetailedByAccountId(id).stream()
                .map(accountManagementMapper::toRoleAssignmentResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<AccountRoleAssignmentResponse> assignRole(String id, AssignRoleRequest request) {
        Account account = getAccountOrThrow(id);
        assertCanAccessAccount(account);
        assertCanManageTargetScope(request.getScopeType(), request.getBrandId(), request.getBranchId(), null);
        createAssignment(account, request.getRoleCode(), request.getScopeType(), request.getBrandId(), request.getBranchId(), request.getExpiresAt());
        log.info("Role assigned by admin: accountId={}, roleCode={}", id, request.getRoleCode());
        return getAccountRoles(id);
    }

    @Override
    @Transactional
    public void revokeRole(String id, String assignmentId) {
        UserPrincipal principal = getCurrentPrincipal();
        if (principal.getId().equals(id)) {
            throw new BaseException(ErrorCode.COM_004, "You cannot revoke your own role assignment");
        }

        AccountRoleAssignment assignment = assignmentRepository.findDetailedById(assignmentId)
                .orElseThrow(() -> new BaseException(ErrorCode.COM_005, "Role assignment not found"));

        if (!assignment.getAccount().getId().equals(id)) {
            throw new BaseException(ErrorCode.COM_004, "Role assignment does not belong to the specified account");
        }

        assertCanAccessAccount(assignment.getAccount());
        assertCanAccessScope(assignment.getScope());

        assignment.setStatus(Constants.STATUS_INACTIVE);
        assignmentRepository.save(assignment);
        refreshTokenRepository.deleteByAccountId(id);
        log.info("Role assignment revoked by admin: assignmentId={}, accountId={}", assignmentId, id);
    }

    private AccountDetailResponse buildAccountDetail(Account account) {
        CustomerProfile customerProfile = customerProfileRepository.findByAccountId(account.getId()).orElse(null);
        List<AccountRoleAssignmentResponse> roleAssignments = assignmentRepository.findDetailedByAccountId(account.getId()).stream()
                .map(accountManagementMapper::toRoleAssignmentResponse)
                .toList();
        return accountManagementMapper.toDetail(account, customerProfile, roleAssignments);
    }

    private void createAssignment(Account account,
                                  String roleCode,
                                  String scopeType,
                                  String brandId,
                                  String branchId,
                                  LocalDateTime expiresAt) {
        Role role = roleRepository.findByCode(roleCode.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BaseException(ErrorCode.ROLE_NOT_FOUND));
        Scope scope = resolveOrCreateScope(scopeType, brandId, branchId);

        if (assignmentRepository.existsActiveValidAssignment(account.getId(), role.getId(), scope.getId(), LocalDateTime.now())) {
            throw new BaseException(ErrorCode.COM_004, "An active assignment with the same role and scope already exists");
        }

        AccountRoleAssignment assignment = new AccountRoleAssignment();
        assignment.setAccount(account);
        assignment.setRole(role);
        assignment.setScope(scope);
        assignment.setStatus(Constants.STATUS_ACTIVE);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setExpiresAt(expiresAt);
        assignmentRepository.save(assignment);
    }

    private Scope resolveOrCreateScope(String rawScopeType, String rawBrandId, String rawBranchId) {
        String scopeType = normalizeScopeType(rawScopeType);
        String brandId = normalizeNullable(rawBrandId);
        String branchId = normalizeNullable(rawBranchId);

        if (Constants.SCOPE_SYSTEM.equals(scopeType)) {
            return scopeRepository.findByScopeTypeAndBrandIdAndBranchId(Constants.SCOPE_SYSTEM, null, null)
                    .orElseThrow(() -> new BaseException(ErrorCode.SCOPE_NOT_FOUND));
        }

        if (Constants.SCOPE_BRAND.equals(scopeType)) {
            if (brandId == null) {
                throw new BaseException(ErrorCode.COM_004, "brandId is required for BRAND scope");
            }
            Brand brand = brandRepository.findById(brandId)
                    .orElseThrow(() -> new BaseException(ErrorCode.BRANCH_003));
            return scopeRepository.findByScopeTypeAndBrandId(Constants.SCOPE_BRAND, brandId)
                    .orElseGet(() -> {
                        Scope scope = new Scope();
                        scope.setScopeType(Constants.SCOPE_BRAND);
                        scope.setBrand(brand);
                        scope.setBranch(null);
                        scope.setStatus(Constants.STATUS_ACTIVE);
                        return scopeRepository.save(scope);
                    });
        }

        if (Constants.SCOPE_BRANCH.equals(scopeType)) {
            if (branchId == null) {
                throw new BaseException(ErrorCode.COM_004, "branchId is required for BRANCH scope");
            }
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new BaseException(ErrorCode.BRANCH_001));
            return scopeRepository.findByScopeTypeAndBranchId(Constants.SCOPE_BRANCH, branchId)
                    .orElseGet(() -> {
                        Scope scope = new Scope();
                        scope.setScopeType(Constants.SCOPE_BRANCH);
                        scope.setBrand(branch.getBrand());
                        scope.setBranch(branch);
                        scope.setStatus(Constants.STATUS_ACTIVE);
                        return scopeRepository.save(scope);
                    });
        }

        throw new BaseException(ErrorCode.COM_004, "Unsupported scope type: " + scopeType);
    }

    private void validateUniqueConstraints(String username, String email, String phone, String accountId) {
        if (accountId == null) {
            if (accountRepository.existsByUsername(username)) {
                throw new BaseException(ErrorCode.AUTH_013);
            }
            if (email != null && accountRepository.existsByEmail(email)) {
                throw new BaseException(ErrorCode.AUTH_014);
            }
            if (phone != null && accountRepository.existsByPhone(phone)) {
                throw new BaseException(ErrorCode.AUTH_015);
            }
            return;
        }

        if (email != null && accountRepository.existsByEmailAndIdNot(email, accountId)) {
            throw new BaseException(ErrorCode.AUTH_014);
        }
        if (phone != null && accountRepository.existsByPhoneAndIdNot(phone, accountId)) {
            throw new BaseException(ErrorCode.AUTH_015);
        }
    }

    private Map<String, List<String>> loadActiveRoles(List<Account> accounts) {
        if (accounts.isEmpty()) {
            return Map.of();
        }

        List<String> accountIds = accounts.stream()
                .map(Account::getId)
                .toList();

        Map<String, List<String>> rolesByAccountId = assignmentRepository
                .findActiveAssignmentsByAccountIdIn(accountIds, LocalDateTime.now())
                .stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getAccount().getId(),
                        Collectors.mapping(
                                assignment -> assignment.getRole().getCode(),
                                Collectors.collectingAndThen(Collectors.toList(), roleCodes -> roleCodes.stream()
                                        .distinct()
                                        .toList())
                        )
                ));

        return accountIds.stream()
                .collect(Collectors.toMap(
                        accountId -> accountId,
                        accountId -> rolesByAccountId.getOrDefault(accountId, List.of())
                ));
    }

    private Specification<Account> buildSearchSpecification(String keyword,
                                                            String status,
                                                            String roleCode,
                                                            String brandId,
                                                            AccessScopeContext accessScope) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("phone")), pattern),
                        cb.like(cb.lower(root.get("fullName")), pattern)
                ));
            }

            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status.trim().toUpperCase(Locale.ROOT)));
            }

            String normalizedBrandId = normalizeNullable(brandId);
            if (normalizedBrandId != null) {
                predicates.add(cb.equal(root.join("brand", JoinType.LEFT).get("id"), normalizedBrandId));
            }

            if (!accessScope.fullAccess()) {
                if (accessScope.brandIds().isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.join("brand", JoinType.LEFT).get("id").in(accessScope.brandIds()));
                }
            }

            if (roleCode != null && !roleCode.trim().isEmpty()) {
                Subquery<String> subquery = query.subquery(String.class);
                var assignmentRoot = subquery.from(AccountRoleAssignment.class);
                var roleJoin = assignmentRoot.join("role");
                subquery.select(assignmentRoot.get("account").get("id"));
                subquery.where(
                        cb.equal(assignmentRoot.get("account").get("id"), root.get("id")),
                        cb.equal(assignmentRoot.get("status"), Constants.STATUS_ACTIVE),
                        cb.or(
                                cb.isNull(assignmentRoot.get("expiresAt")),
                                cb.greaterThan(assignmentRoot.get("expiresAt"), LocalDateTime.now())
                        ),
                        cb.equal(cb.upper(roleJoin.get("code")), roleCode.trim().toUpperCase(Locale.ROOT))
                );
                predicates.add(cb.exists(subquery));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private Brand resolveAccountBrand(String accountBrandId, String scopeType, String scopeBrandId, String scopeBranchId) {
        String normalizedAccountBrandId = normalizeNullable(accountBrandId);
        if (normalizedAccountBrandId != null) {
            return resolveBrandOrNull(normalizedAccountBrandId);
        }

        String normalizedScopeType = normalizeScopeType(scopeType);
        if (Constants.SCOPE_BRAND.equals(normalizedScopeType) && normalizeNullable(scopeBrandId) != null) {
            return resolveBrandOrNull(scopeBrandId);
        }
        if (Constants.SCOPE_BRANCH.equals(normalizedScopeType) && normalizeNullable(scopeBranchId) != null) {
            Branch branch = branchRepository.findById(scopeBranchId)
                    .orElseThrow(() -> new BaseException(ErrorCode.BRANCH_001));
            return branch.getBrand();
        }
        return null;
    }

    private Brand resolveBrandOrNull(String brandId) {
        String normalized = normalizeNullable(brandId);
        if (normalized == null) {
            return null;
        }
        return brandRepository.findById(normalized)
                .orElseThrow(() -> new BaseException(ErrorCode.BRANCH_003));
    }

    private void syncCustomerProfile(Account account) {
        customerProfileRepository.findByAccountId(account.getId()).ifPresent(profile -> {
            profile.setFullName(account.getFullName());
            profile.setPhone(account.getPhone());
            profile.setEmail(account.getEmail());
            customerProfileRepository.save(profile);
        });
    }

    private Account getAccountOrThrow(String id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.AUTH_012));
    }

    private void assertCanAccessAccount(Account targetAccount) {
        AccessScopeContext accessScope = resolveAccessScope();
        if (accessScope.fullAccess()) {
            return;
        }
        String targetBrandId = targetAccount.getBrand() != null ? targetAccount.getBrand().getId() : null;
        if (targetBrandId == null || !accessScope.brandIds().contains(targetBrandId)) {
            throw new BaseException(ErrorCode.AUTH_007);
        }
    }

    private void assertCanManageTargetScope(String scopeType, String brandId, String branchId, String accountBrandId) {
        AccessScopeContext accessScope = resolveAccessScope();
        if (accessScope.fullAccess()) {
            return;
        }

        String normalizedScopeType = normalizeScopeType(scopeType);
        if (Constants.SCOPE_SYSTEM.equals(normalizedScopeType)) {
            throw new BaseException(ErrorCode.AUTH_007, "Insufficient permissions to manage SYSTEM scope");
        }

        String targetBrandId = resolveTargetBrandIdForScope(normalizedScopeType, brandId, branchId, accountBrandId);
        if (targetBrandId == null || !accessScope.brandIds().contains(targetBrandId)) {
            throw new BaseException(ErrorCode.AUTH_007);
        }
    }

    private void assertCanAccessScope(Scope scope) {
        AccessScopeContext accessScope = resolveAccessScope();
        if (accessScope.fullAccess()) {
            return;
        }

        if (Constants.SCOPE_SYSTEM.equals(scope.getScopeType())) {
            throw new BaseException(ErrorCode.AUTH_007, "Insufficient permissions to manage SYSTEM scope");
        }

        String targetBrandId = null;
        if (scope.getBrand() != null) {
            targetBrandId = scope.getBrand().getId();
        } else if (scope.getBranch() != null && scope.getBranch().getBrand() != null) {
            targetBrandId = scope.getBranch().getBrand().getId();
        }

        if (targetBrandId == null || !accessScope.brandIds().contains(targetBrandId)) {
            throw new BaseException(ErrorCode.AUTH_007);
        }
    }

    private String resolveTargetBrandIdForScope(String scopeType, String brandId, String branchId, String accountBrandId) {
        String normalizedAccountBrandId = normalizeNullable(accountBrandId);
        if (normalizedAccountBrandId != null) {
            return normalizedAccountBrandId;
        }

        String normalizedBrandId = normalizeNullable(brandId);
        if (Constants.SCOPE_BRAND.equals(scopeType)) {
            return normalizedBrandId;
        }
        if (Constants.SCOPE_BRANCH.equals(scopeType)) {
            String normalizedBranchId = normalizeNullable(branchId);
            if (normalizedBranchId == null) {
                return null;
            }
            Branch branch = branchRepository.findById(normalizedBranchId)
                    .orElseThrow(() -> new BaseException(ErrorCode.BRANCH_001));
            return branch.getBrand() != null ? branch.getBrand().getId() : null;
        }
        return normalizedBrandId;
    }

    private AccessScopeContext resolveAccessScope() {
        UserPrincipal principal = getCurrentPrincipal();
        List<AccountRoleAssignment> assignments = assignmentRepository.findActiveAssignmentsByAccountId(principal.getId(), LocalDateTime.now());

        boolean fullAccess = assignments.stream().anyMatch(assignment ->
                Constants.SCOPE_SYSTEM.equals(assignment.getScope().getScopeType())
        );
        if (fullAccess) {
            return new AccessScopeContext(true, Set.of());
        }

        Set<String> brandIds = new HashSet<>();
        for (AccountRoleAssignment assignment : assignments) {
            Scope scope = assignment.getScope();
            if (scope.getBrand() != null && scope.getBrand().getId() != null) {
                brandIds.add(scope.getBrand().getId());
            } else if (scope.getBranch() != null && scope.getBranch().getBrand() != null) {
                brandIds.add(scope.getBranch().getBrand().getId());
            }
        }
        return new AccessScopeContext(false, brandIds);
    }

    private UserPrincipal getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BaseException(ErrorCode.AUTH_003);
        }
        return principal;
    }

    private String normalizeUsername(String username) {
        return username.trim();
    }

    private String normalizeEmail(String email) {
        String normalized = normalizeNullable(email);
        return normalized != null ? normalized.toLowerCase(Locale.ROOT) : null;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeStatus(String status) {
        String normalized = normalizeNullable(status);
        if (normalized == null) {
            return Constants.STATUS_ACTIVE;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (!Set.of(Constants.STATUS_ACTIVE, Constants.STATUS_INACTIVE, Constants.STATUS_LOCKED).contains(upper)) {
            throw new BaseException(ErrorCode.COM_004, "Unsupported account status: " + status);
        }
        return upper;
    }

    private String normalizeScopeType(String scopeType) {
        String normalized = normalizeNullable(scopeType);
        if (normalized == null) {
            return Constants.SCOPE_SYSTEM;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (!Set.of(Constants.SCOPE_SYSTEM, Constants.SCOPE_BRAND, Constants.SCOPE_BRANCH).contains(upper)) {
            throw new BaseException(ErrorCode.COM_004, "Unsupported scope type: " + scopeType);
        }
        return upper;
    }

    private record AccessScopeContext(boolean fullAccess, Set<String> brandIds) {
    }
}
