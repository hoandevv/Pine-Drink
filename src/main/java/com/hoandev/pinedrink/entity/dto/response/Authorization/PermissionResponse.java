package com.hoandev.pinedrink.entity.dto.response.Authorization;

public record PermissionResponse(
        String code,
        String name,
        String module,
        String description,
        String status
) {
}
