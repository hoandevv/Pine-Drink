package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.Authorization.UpdateRolePermissionsRequest;
import com.hoandev.pinedrink.entity.dto.response.Authorization.PermissionResponse;
import com.hoandev.pinedrink.entity.dto.response.Authorization.RolePermissionsMatrixResponse;
import com.hoandev.pinedrink.entity.dto.response.Authorization.RoleResponse;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.service.AuthorizationManagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/authorization")
public class AuthorizationManagementController {

    private final AuthorizationManagementService authorizationManagementService;

    public AuthorizationManagementController(AuthorizationManagementService authorizationManagementService) {
        this.authorizationManagementService = authorizationManagementService;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('PERM_ROLE_VIEW')")
    public ResponseEntity<BaseResponse<List<RoleResponse>>> getRoles() {
        return ResponseEntity.ok(BaseResponse.success(
                authorizationManagementService.getRoles(),
                "Roles retrieved successfully"
        ));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('PERM_PERMISSION_VIEW')")
    public ResponseEntity<BaseResponse<List<PermissionResponse>>> getPermissions() {
        return ResponseEntity.ok(BaseResponse.success(
                authorizationManagementService.getPermissions(),
                "Permissions retrieved successfully"
        ));
    }

    @GetMapping("/role-permissions/matrix")
    @PreAuthorize("hasAuthority('PERM_ROLE_PERMISSION_VIEW')")
    public ResponseEntity<BaseResponse<RolePermissionsMatrixResponse>> getRolePermissionsMatrix() {
        return ResponseEntity.ok(BaseResponse.success(
                authorizationManagementService.getRolePermissionsMatrix(),
                "Role permissions matrix retrieved successfully"
        ));
    }

    @PutMapping("/roles/{roleCode}/permissions")
    @PreAuthorize("hasAuthority('PERM_ROLE_PERMISSION_UPDATE')")
    public ResponseEntity<BaseResponse<RolePermissionsMatrixResponse>> updateRolePermissions(
            @PathVariable String roleCode,
            @RequestBody @Valid UpdateRolePermissionsRequest request) {
        return ResponseEntity.ok(BaseResponse.success(
                authorizationManagementService.updateRolePermissions(roleCode, request),
                "Role permissions updated successfully"
        ));
    }
}
