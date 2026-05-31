package com.hoandev.pinedrink.entity.dto.response.Authorization;

import java.util.List;
import java.util.Map;

public record RolePermissionsMatrixResponse(
        List<RoleResponse> roles,
        List<PermissionResponse> permissions,
        Map<String, List<String>> matrix
) {
}
