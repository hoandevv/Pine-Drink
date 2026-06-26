package com.hoandev.pinedrink.entity.dto.request.Payment;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MomoCreatePaymentRequest {
    @NotBlank(message = "Order id is required")
    private String orderId;

    private String orderInfo;
    private String extraData;
}
