package com.hoandev.pinedrink.entity.dto.response.Authorization;

public record RoleResponse(
        String code,
        String name,
        String description,
        String roleType,
        String status,
        boolean editable
) {
}
