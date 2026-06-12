package com.hoandev.pinedrink.entity.dto.request.Voucher;

import com.hoandev.pinedrink.entity.enums.EntityStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateVoucherStatusRequest {

    @NotNull(message = "Status is required")
    private EntityStatus status;
}
