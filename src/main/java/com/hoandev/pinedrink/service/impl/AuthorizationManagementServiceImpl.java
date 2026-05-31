package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.Permission;
import com.hoandev.pinedrink.entity.Role;
import com.hoandev.pinedrink.entity.RolePermission;
import com.hoandev.pinedrink.entity.dto.request.Authorization.UpdateRolePermissionsRequest;
import com.hoandev.pinedrink.entity.dto.response.Authorization.PermissionResponse;
import com.hoandev.pinedrink.entity.dto.response.Authorization.RolePermissionsMatrixResponse;
import com.hoandev.pinedrink.entity.dto.response.Authorization.RoleResponse;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.repository.AccountRoleAssignmentRepository;
import com.hoandev.pinedrink.repository.PermissionRepository;
import com.hoandev.pinedrink.repository.RolePermissionRepository;
import com.hoandev.pinedrink.repository.RoleRepository;
import com.hoandev.pinedrink.service.AuthorizationManagementService;
import com.hoandev.pinedrink.service.PermissionCacheService;
import com.hoandev.pinedrink.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationManagementServiceImpl implements AuthorizationManagementService {

    private static final String PROTECTED_ROLE_ADMIN = Constants.ROLE_ADMIN;

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final AccountRoleAssignmentRepository assignmentRepository;
    private final PermissionCacheService permissionCacheService;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles() {
        return roleRepository.findByStatusOrderByCodeAsc(Constants.STATUS_ACTIVE)
                .stream()
                .map(this::toRoleResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getPermissions() {
        return permissionRepository.findByStatusOrderByModuleAscCodeAsc(Constants.STATUS_ACTIVE)
                .stream()
                .map(this::toPermissionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RolePermissionsMatrixResponse getRolePermissionsMatrix() {
        List<RoleResponse> roles = getRoles();
        List<PermissionResponse> permissions = getPermissions();
        Map<String, List<String>> matrix = buildMatrix();
        return new RolePermissionsMatrixResponse(roles, permissions, matrix);
    }

    @Override
    @Transactional
    public RolePermissionsMatrixResponse updateRolePermissions(String roleCode, UpdateRolePermissionsRequest request) {
        String normalizedRoleCode = normalizeCode(roleCode);
        if (PROTECTED_ROLE_ADMIN.equals(normalizedRoleCode)) {
            throw new BaseException(ErrorCode.AUTH_007, "ADMIN role permissions are protected");
        }

        Role role = roleRepository.findByCode(normalizedRoleCode)
                .orElseThrow(() -> new BaseException(ErrorCode.ROLE_NOT_FOUND));

        Set<String> requestedCodes = request.permissions().stream()
                .map(this::normalizeCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Permission> permissions = permissionRepository.findByCodeInAndStatus(
                new ArrayList<>(requestedCodes), Constants.STATUS_ACTIVE);
        Map<String, Permission> permissionByCode = permissions.stream()
                .collect(Collectors.toMap(Permission::getCode, Function.identity()));

        List<String> missingCodes = requestedCodes.stream()
                .filter(code -> !permissionByCode.containsKey(code))
                .toList();
        if (!missingCodes.isEmpty()) {
            throw new BaseException(ErrorCode.COM_004, "Invalid permission codes: " + String.join(", ", missingCodes));
        }

        List<RolePermission> existingEntries = rolePermissionRepository.findByRoleId(role.getId());
        Map<String, RolePermission> existingByPermissionCode = existingEntries.stream()
                .collect(Collectors.toMap(entry -> entry.getPermission().getCode(), Function.identity()));

        for (RolePermission entry : existingEntries) {
            String permissionCode = entry.getPermission().getCode();
            entry.setStatus(requestedCodes.contains(permissionCode) ? Constants.STATUS_ACTIVE : Constants.STATUS_INACTIVE);
        }

        for (String permissionCode : requestedCodes) {
            if (!existingByPermissionCode.containsKey(permissionCode)) {
                RolePermission entry = new RolePermission();
                entry.setRole(role);
                entry.setPermission(permissionByCode.get(permissionCode));
                entry.setStatus(Constants.STATUS_ACTIVE);
                existingEntries.add(entry);
            }
        }

        rolePermissionRepository.saveAll(existingEntries);
        invalidateAssignedAccountCaches(role);

        log.info("Role permissions updated: roleCode={}, permissionCount={}", normalizedRoleCode, requestedCodes.size());
        return getRolePermissionsMatrix();
    }

    private Map<String, List<String>> buildMatrix() {
        Map<String, List<String>> matrix = new LinkedHashMap<>();
        roleRepository.findByStatusOrderByCodeAsc(Constants.STATUS_ACTIVE)
                .forEach(role -> matrix.put(role.getCode(), new ArrayList<>()));

        rolePermissionRepository.findActiveMatrixEntries().forEach(entry ->
                matrix.computeIfAbsent(entry.getRole().getCode(), ignored -> new ArrayList<>())
                        .add(entry.getPermission().getCode())
        );
        return matrix;
    }

    private void invalidateAssignedAccountCaches(Role role) {
        List<String> accountIds = assignmentRepository.findActiveAccountIdsByRoleId(role.getId(), LocalDateTime.now());
        if (!accountIds.isEmpty()) {
            permissionCacheService.invalidateUserCaches(accountIds);
        }
    }

    private RoleResponse toRoleResponse(Role role) {
        return new RoleResponse(
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.getRoleType(),
                role.getStatus(),
                !PROTECTED_ROLE_ADMIN.equals(role.getCode())
        );
    }

    private PermissionResponse toPermissionResponse(Permission permission) {
        return new PermissionResponse(
                permission.getCode(),
                permission.getName(),
                permission.getModule(),
                permission.getDescription(),
                permission.getStatus()
        );
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return "";
        }
        String normalized = code.trim().toUpperCase();
        if (normalized.startsWith("PERM_")) {
            return normalized.substring("PERM_".length());
        }
        return normalized;
    }
}
