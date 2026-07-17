package com.hoandev.pinedrink.entity.dto.request.Authorization;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateRolePermissionsRequest(
        @NotNull List<String> permissions
) {
}
