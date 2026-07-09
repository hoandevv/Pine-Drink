package com.hoandev.pinedrink.entity.dto.request.Payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordOfflinePaymentRequest {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "CASH|COD|BANK_TRANSFER", message = "Payment method must be CASH, COD or BANK_TRANSFER")
    private String paymentMethod;
}
