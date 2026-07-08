package com.hoandev.pinedrink.entity.dto.request.Order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "PENDING|CONFIRMED|PREPARING|READY|DELIVERING|COMPLETED|CANCELLED|REJECTED", 
            message = "Invalid order status")
    private String status;

    private String reason;

    @Pattern(regexp = "CASH|COD|BANK_TRANSFER", message = "Payment method must be CASH, COD or BANK_TRANSFER")
    private String paymentMethod;
}
