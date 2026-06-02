package com.hoandev.pinedrink.entity.dto.request.Topping;

import com.hoandev.pinedrink.entity.enums.ToppingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateToppingStatusRequest {

    @NotNull(message = "Status is required")
    private ToppingStatus status;
}
