package com.hoandev.pinedrink.entity.dto.request.ProductTopping;

import com.hoandev.pinedrink.entity.enums.ProductVariantStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProductVariantStatusRequest {

    @NotNull(message = "Status is required")
    private ProductVariantStatus status;
}
