package com.hoandev.pinedrink.entity.dto.request.Voucher;

import com.hoandev.pinedrink.entity.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVoucherRequest {

    @NotBlank(message = "Voucher code is required")
    @Size(max = 100, message = "Voucher code must be at most 100 characters")
    private String code;

    @NotBlank(message = "Voucher name is required")
    @Size(max = 150, message = "Voucher name must be at most 150 characters")
    private String name;

    @Size(max = 255, message = "Voucher description must be at most 255 characters")
    private String description;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Discount value is invalid")
    private BigDecimal discountValue;

    @Digits(integer = 10, fraction = 2, message = "Max discount amount is invalid")
    private BigDecimal maxDiscountAmount;

    @DecimalMin(value = "0.00", message = "Min order amount must be greater than or equal to 0")
    @Digits(integer = 10, fraction = 2, message = "Min order amount is invalid")
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    private Integer usageLimit;

    private Integer usageLimitPerCustomer;

    @NotNull(message = "Start time is required")
    private LocalDateTime startAt;

    @NotNull(message = "End time is required")
    private LocalDateTime endAt;

    private List<@NotBlank(message = "Branch id must not be blank") String> branchIds;
}
