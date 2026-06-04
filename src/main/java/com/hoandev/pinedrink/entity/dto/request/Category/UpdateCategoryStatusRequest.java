package com.hoandev.pinedrink.entity.dto.request.Category;

import com.hoandev.pinedrink.entity.enums.CategoryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCategoryStatusRequest {

    @NotNull(message = "Status is required")
    private CategoryStatus status;
}
