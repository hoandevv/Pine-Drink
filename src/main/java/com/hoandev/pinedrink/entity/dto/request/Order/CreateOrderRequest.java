package com.hoandev.pinedrink.entity.dto.request.Order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotBlank(message = "Branch ID is required")
    private String branchId;

    @NotBlank(message = "Order type is required")
    @Pattern(regexp = "PICKUP|DELIVERY", message = "Order type must be PICKUP or DELIVERY")
    private String orderType;

    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "CASH|COD|VNPAY|MOMO|BANK_TRANSFER", message = "Invalid payment method")
    private String paymentMethod;

    private LocalDateTime pickupTime;

    private String deliveryAddressId;

    private String note;

    private String voucherCode;
}
