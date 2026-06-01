    package com.hoandev.pinedrink.service.impl;
    
    import com.hoandev.pinedrink.entity.Account;
    import com.hoandev.pinedrink.entity.AccountRoleAssignment;
    import com.hoandev.pinedrink.entity.Branch;
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
    import com.hoandev.pinedrink.repository.CustomerProfileRepository;
    import com.hoandev.pinedrink.repository.RefreshTokenRepository;
    import com.hoandev.pinedrink.repository.RoleRepository;
    import com.hoandev.pinedrink.repository.ScopeRepository;
import com.hoandev.pinedrink.security.UserPrincipal;
import com.hoandev.pinedrink.service.AccountService;
import com.hoandev.pinedrink.service.AccessScopeService;
import com.hoandev.pinedrink.service.PermissionCacheService;
import com.hoandev.pinedrink.security.scope.AccessScopeContext;
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
        private final BranchRepository branchRepository;
        private final CustomerProfileRepository customerProfileRepository;
        private final RefreshTokenRepository refreshTokenRepository;
        private final PasswordEncoder passwordEncoder;
        private final AccountManagementMapper accountManagementMapper;
        private final PermissionCacheService permissionCacheService;
        private final AccessScopeService accessScopeService;
    
        @Override
        @Transactional(readOnly = true)
        public PageResponse<AccountListItemResponse> searchAccounts(String keyword,
                                                                    String status,
                                                                    String roleCode,
                                                                    Pageable pageable) {
            AccessScopeContext accessScope = accessScopeService.resolveCurrentScope();
            Specification<Account> specification = buildSearchSpecification(keyword, status, roleCode, accessScope);
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
            accessScopeService.assertCanAccessAccount(account.getId());
            return buildAccountDetail(account);
        }
    
        @Override
        @Transactional
        public AccountDetailResponse createAccount(CreateAccountRequest request) {
            validateRoleScopePolicy(request.getRoleCode(), request.getScopeType(), request.getScopeBranchId());
            accessScopeService.assertCanManageTargetScope(request.getScopeType(), request.getScopeBranchId());
    
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
            Account savedAccount = accountRepository.save(account);
            createAssignment(savedAccount, request.getRoleCode(), request.getScopeType(), request.getScopeBranchId(), request.getExpiresAt());
            permissionCacheService.invalidateUserCache(savedAccount.getId());

            log.info("Account created by admin: id={}, username={}", savedAccount.getId(), savedAccount.getUsername());
            return buildAccountDetail(savedAccount);
        }
    
        @Override
        @Transactional
        public AccountDetailResponse updateAccount(String id, UpdateAccountRequest request) {
            Account account = getAccountOrThrow(id);
            accessScopeService.assertCanAccessAccount(account.getId());
    
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
            accessScopeService.assertCanAccessAccount(account.getId());
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
            accessScopeService.assertCanAccessAccount(account.getId());
            account.setPassword(passwordEncoder.encode(request.getNewPassword()));
            accountRepository.save(account);
            refreshTokenRepository.deleteByAccountId(account.getId());
            log.info("Account password reset by admin: id={}", account.getId());
        }
    
        @Override
        @Transactional(readOnly = true)
        public List<AccountRoleAssignmentResponse> getAccountRoles(String id) {
            Account account = getAccountOrThrow(id);
            accessScopeService.assertCanAccessAccount(account.getId());
            return assignmentRepository.findDetailedByAccountId(id).stream()
                    .map(accountManagementMapper::toRoleAssignmentResponse)
                    .toList();
        }
    
        @Override
        @Transactional
        public List<AccountRoleAssignmentResponse> assignRole(String id, AssignRoleRequest request) {
            Account account = getAccountOrThrow(id);
            accessScopeService.assertCanAccessAccount(account.getId());
            validateRoleScopePolicy(request.getRoleCode(), request.getScopeType(), request.getBranchId());
            accessScopeService.assertCanManageTargetScope(request.getScopeType(), request.getBranchId());
            createAssignment(account, request.getRoleCode(), request.getScopeType(), request.getBranchId(), request.getExpiresAt());
            permissionCacheService.invalidateUserCache(account.getId());
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
    
            accessScopeService.assertCanAccessAccount(assignment.getAccount().getId());
            accessScopeService.assertCanAccessScope(assignment.getScope());
    
        assignment.setStatus(Constants.STATUS_INACTIVE);
        assignmentRepository.save(assignment);
        refreshTokenRepository.deleteByAccountId(id);
        permissionCacheService.invalidateUserCache(id);
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
                                      String branchId,
                                      LocalDateTime expiresAt) {
            Role role = roleRepository.findByCode(roleCode.trim().toUpperCase(Locale.ROOT))
                    .orElseThrow(() -> new BaseException(ErrorCode.ROLE_NOT_FOUND));
            Scope scope = resolveOrCreateScope(scopeType, branchId);
    
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

        /**
         * Enforces role-to-scope rules before creating or assigning staff accounts.
         * ADMIN and CUSTOMER stay system-scoped; branch-operated roles must target one branch.
         *
         * @param rawRoleCode role code from the request
         * @param rawScopeType scope type from the request
         * @param rawBranchId branch ID from the request, required for BRANCH scope
         */
        private void validateRoleScopePolicy(String rawRoleCode, String rawScopeType, String rawBranchId) {
            String roleCode = normalizeNullable(rawRoleCode);
            if (roleCode == null) {
                throw new BaseException(ErrorCode.COM_004, "roleCode is required");
            }
            roleCode = roleCode.toUpperCase(Locale.ROOT);

            String scopeType = normalizeScopeType(rawScopeType);
            String branchId = normalizeNullable(rawBranchId);

            if (Constants.ROLE_ADMIN.equals(roleCode) || Constants.ROLE_CUSTOMER.equals(roleCode)) {
                if (!Constants.SCOPE_SYSTEM.equals(scopeType) || branchId != null) {
                    throw new BaseException(ErrorCode.COM_004, roleCode + " must use SYSTEM scope without branchId");
                }
                return;
            }

            if (!Constants.SCOPE_BRANCH.equals(scopeType) || branchId == null) {
                throw new BaseException(ErrorCode.COM_004, roleCode + " must use BRANCH scope with branchId");
            }
        }
     
        private Scope resolveOrCreateScope(String rawScopeType, String rawBranchId) {
            String scopeType = normalizeScopeType(rawScopeType);
            String branchId = normalizeNullable(rawBranchId);
            if (Constants.SCOPE_SYSTEM.equals(scopeType)) {
                return scopeRepository.findByScopeTypeAndBranchId(Constants.SCOPE_SYSTEM, null)
                        .orElseThrow(() -> new BaseException(ErrorCode.SCOPE_NOT_FOUND));
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
    
                if (!accessScope.fullAccess()) {
                    if (accessScope.branchIds().isEmpty()) {
                        predicates.add(cb.disjunction());
                    } else {
                        Subquery<String> branchScopeSubquery = query.subquery(String.class);
                        var assignmentRoot = branchScopeSubquery.from(AccountRoleAssignment.class);
                        var scopeJoin = assignmentRoot.join("scope");
                        var branchJoin = scopeJoin.join("branch");
                        branchScopeSubquery.select(assignmentRoot.get("account").get("id"));
                        branchScopeSubquery.where(
                                cb.equal(assignmentRoot.get("account").get("id"), root.get("id")),
                                cb.equal(assignmentRoot.get("status"), Constants.STATUS_ACTIVE),
                                cb.or(
                                        cb.isNull(assignmentRoot.get("expiresAt")),
                                        cb.greaterThan(assignmentRoot.get("expiresAt"), LocalDateTime.now())
                                ),
                                cb.equal(scopeJoin.get("scopeType"), Constants.SCOPE_BRANCH),
                                branchJoin.get("id").in(accessScope.branchIds())
                        );
                        predicates.add(cb.exists(branchScopeSubquery));
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
            if (!Set.of(Constants.SCOPE_SYSTEM, Constants.SCOPE_BRANCH).contains(upper)) {
                throw new BaseException(ErrorCode.COM_004, "Unsupported scope type: " + scopeType);
            }
            return upper;
        }
    }
