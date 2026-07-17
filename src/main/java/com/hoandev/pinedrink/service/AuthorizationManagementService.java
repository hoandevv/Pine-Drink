package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Authorization.UpdateRolePermissionsRequest;
import com.hoandev.pinedrink.entity.dto.response.Authorization.PermissionResponse;
import com.hoandev.pinedrink.entity.dto.response.Authorization.RolePermissionsMatrixResponse;
import com.hoandev.pinedrink.entity.dto.response.Authorization.RoleResponse;

import java.util.List;
/**
 * Service interface for managing authorization-related operations.
 */
public interface AuthorizationManagementService {
    /**
     * Retrieves all roles.
     *
     * @return a list of roles
     */
    List<RoleResponse> getRoles();

    /**
     * Retrieves all permissions.
     *
     * @return a list of permissions
     */
    List<PermissionResponse> getPermissions();

    /**
     * Retrieves the role permissions matrix.
     *
     * @return the role permissions matrix
     */
    RolePermissionsMatrixResponse getRolePermissionsMatrix();
    /**
     * Updates the permissions for a specific role.
     *
     * @param roleCode the code of the role
     * @param request the request containing the updated permissions
     * @return the updated role permissions matrix
     */
    RolePermissionsMatrixResponse updateRolePermissions(String roleCode, UpdateRolePermissionsRequest request);
}
