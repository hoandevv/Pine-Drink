package com.hoandev.pinedrink.entity.dto.request.ProductTopping;

import com.hoandev.pinedrink.entity.enums.ProductToppingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProductToppingStatusRequest {

    @NotNull(message = "Status is required")
    private ProductToppingStatus status;
}
